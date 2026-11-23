package com.cinebook.service;

import com.cinebook.dto.BookingResponse;
import com.cinebook.dto.PaymentConfirmRequest;
import com.cinebook.dto.PaymentOrderResponse;
import com.cinebook.entity.Booking;
import com.cinebook.entity.BookingStatus;
import com.cinebook.exception.BadRequestException;
import com.cinebook.exception.ForbiddenException;
import com.cinebook.security.UserPrincipal;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Payment module.
 * - RAZORPAY: when RAZORPAY_KEY_ID / RAZORPAY_KEY_SECRET are set, a Razorpay order is created and the
 *   checkout signature is verified with HMAC-SHA256 before the booking is confirmed.
 * - SIMULATED: without keys, payment confirmation is simulated (test mode) so the app works end to end.
 */
@Service
public class PaymentService {

    static final String RAZORPAY = "RAZORPAY";
    static final String SIMULATED = "SIMULATED";
    /** Small allowance for the payment popup closing right at the end of the hold. */
    static final int GRACE_SECONDS = 60;

    private final BookingService bookingService;
    private final ObjectMapper objectMapper;
    private final String keyId;
    private final String keySecret;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public PaymentService(BookingService bookingService, ObjectMapper objectMapper,
                          @Value("${app.razorpay.key-id:}") String keyId,
                          @Value("${app.razorpay.key-secret:}") String keySecret) {
        this.bookingService = bookingService;
        this.objectMapper = objectMapper;
        this.keyId = keyId;
        this.keySecret = keySecret;
    }

    public boolean razorpayEnabled() { return !keyId.isBlank() && !keySecret.isBlank(); }

    @Transactional
    public PaymentOrderResponse createOrder(Long bookingId, UserPrincipal me) {
        Booking b = payableBooking(bookingId, me);
        long paise = b.getAmount().multiply(BigDecimal.valueOf(100)).longValueExact();
        String description = b.getShow().getMovie().getTitle() + " - " + b.getSeatLabels().replace(",", ", ");
        String orderId;
        String provider;
        if (razorpayEnabled()) {
            provider = RAZORPAY;
            orderId = createRazorpayOrder(paise, b.getReference());
        } else {
            provider = SIMULATED;
            orderId = "sim_order_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
        }
        b.setPaymentProvider(provider);
        b.setPaymentOrderId(orderId);
        return new PaymentOrderResponse(b.getId(), provider, razorpayEnabled() ? keyId : null, orderId, paise, "INR",
                b.getUser().getName(), b.getUser().getEmail(), description);
    }

    @Transactional
    public BookingResponse confirm(Long bookingId, PaymentConfirmRequest req, UserPrincipal me) {
        Booking b = payableBooking(bookingId, me);
        if (b.getPaymentOrderId() == null) throw new BadRequestException("Start the payment first");
        String paymentId;
        if (RAZORPAY.equals(b.getPaymentProvider())) {
            if (req == null || req.razorpayPaymentId() == null || req.razorpaySignature() == null
                    || !b.getPaymentOrderId().equals(req.razorpayOrderId())) {
                throw new BadRequestException("Payment details are missing");
            }
            if (!verifySignature(b.getPaymentOrderId(), req.razorpayPaymentId(), req.razorpaySignature())) {
                throw new BadRequestException("Payment verification failed");
            }
            paymentId = req.razorpayPaymentId();
        } else {
            paymentId = "sim_pay_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
        }
        Booking confirmed = bookingService.confirm(b, b.getPaymentProvider(), b.getPaymentOrderId(), paymentId);
        return BookingResponse.from(confirmed, bookingService.now(), true);
    }

    /** HMAC_SHA256(order_id + "|" + payment_id, key_secret) must equal the signature Razorpay sent. */
    boolean verifySignature(String orderId, String paymentId, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(keySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expected = HexFormat.of().formatHex(mac.doFinal((orderId + "|" + paymentId).getBytes(StandardCharsets.UTF_8)));
            return java.security.MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }

    private Booking payableBooking(Long bookingId, UserPrincipal me) {
        Booking b = bookingService.find(bookingId);
        if (!b.getUser().getId().equals(me.getId())) throw new ForbiddenException("This is not your booking");
        if (b.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new BadRequestException("This booking is " + b.getStatus().name().toLowerCase().replace('_', ' '));
        }
        if (bookingService.now().isAfter(b.getHoldExpiresAt().plusSeconds(GRACE_SECONDS))) {
            bookingService.expireHolds();
            throw new BadRequestException("Your seat hold has expired. Please select seats again.");
        }
        return b;
    }

    private String createRazorpayOrder(long paise, String receipt) {
        try {
            String body = objectMapper.writeValueAsString(java.util.Map.of(
                    "amount", paise, "currency", "INR", "receipt", receipt, "payment_capture", 1));
            String auth = Base64.getEncoder().encodeToString((keyId + ":" + keySecret).getBytes(StandardCharsets.UTF_8));
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.razorpay.com/v1/orders"))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Basic " + auth)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> res = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() / 100 != 2) throw new BadRequestException("Payment gateway error: " + res.body());
            JsonNode json = objectMapper.readTree(res.body());
            return json.get("id").asText();
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Could not reach the payment gateway: " + e.getMessage());
        }
    }
}
