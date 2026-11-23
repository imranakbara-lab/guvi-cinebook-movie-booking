package com.cinebook.service;

import com.cinebook.TestData;
import com.cinebook.dto.PaymentConfirmRequest;
import com.cinebook.dto.PaymentOrderResponse;
import com.cinebook.entity.*;
import com.cinebook.exception.BadRequestException;
import com.cinebook.exception.ForbiddenException;
import com.cinebook.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 1, 12, 0);

    @Mock private BookingService bookingService;

    private final UserPrincipal me = new UserPrincipal(5L, "u@mail.com", "x", "USER");
    private Booking booking;

    @BeforeEach
    void setUp() {
        booking = TestData.booking(TestData.user(5L, "u@mail.com", Role.USER), TestData.show(NOW.plusDays(1)),
                BookingStatus.PENDING_PAYMENT, NOW.minusMinutes(2));
        lenient().when(bookingService.now()).thenReturn(NOW);
        lenient().when(bookingService.find(100L)).thenReturn(booking);
    }

    private PaymentService simulated() { return new PaymentService(bookingService, new ObjectMapper(), "", ""); }
    private PaymentService razorpay() { return new PaymentService(bookingService, new ObjectMapper(), "rzp_test_key", "secret123"); }

    private static String sign(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void createOrder_withoutKeys_isSimulated() {
        PaymentOrderResponse res = simulated().createOrder(100L, me);

        assertThat(res.provider()).isEqualTo("SIMULATED");
        assertThat(res.amountInPaise()).isEqualTo(20000);
        assertThat(res.keyId()).isNull();
        assertThat(booking.getPaymentOrderId()).startsWith("sim_order_");
    }

    @Test
    void confirm_simulated_confirmsBooking() {
        PaymentService service = simulated();
        service.createOrder(100L, me);
        when(bookingService.confirm(eq(booking), eq("SIMULATED"), anyString(), anyString())).thenReturn(booking);

        service.confirm(100L, null, me);

        verify(bookingService).confirm(eq(booking), eq("SIMULATED"), startsWith("sim_order_"), startsWith("sim_pay_"));
    }

    @Test
    void confirm_beforeOrder_rejected() {
        assertThatThrownBy(() -> simulated().confirm(100L, null, me)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void razorpaySignature_validAndInvalid() throws Exception {
        PaymentService service = razorpay();
        String good = sign("order_1|pay_1", "secret123");

        assertThat(service.verifySignature("order_1", "pay_1", good)).isTrue();
        assertThat(service.verifySignature("order_1", "pay_2", good)).isFalse();
        assertThat(service.verifySignature("order_1", "pay_1", "tampered")).isFalse();
    }

    @Test
    void confirm_razorpay_rejectsBadSignature() {
        booking.setPaymentProvider("RAZORPAY");
        booking.setPaymentOrderId("order_1");

        assertThatThrownBy(() -> razorpay().confirm(100L, new PaymentConfirmRequest("order_1", "pay_1", "bad"), me))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("verification");
        verify(bookingService, never()).confirm(any(), any(), any(), any());
    }

    @Test
    void confirm_razorpay_validSignatureConfirms() throws Exception {
        booking.setPaymentProvider("RAZORPAY");
        booking.setPaymentOrderId("order_1");
        when(bookingService.confirm(booking, "RAZORPAY", "order_1", "pay_1")).thenReturn(booking);

        razorpay().confirm(100L, new PaymentConfirmRequest("order_1", "pay_1", sign("order_1|pay_1", "secret123")), me);

        verify(bookingService).confirm(booking, "RAZORPAY", "order_1", "pay_1");
    }

    @Test
    void pay_afterHoldExpired_rejected() {
        when(bookingService.now()).thenReturn(NOW.plusMinutes(20));

        assertThatThrownBy(() -> simulated().createOrder(100L, me)).isInstanceOf(BadRequestException.class).hasMessageContaining("expired");
        verify(bookingService).expireHolds();
    }

    @Test
    void pay_someoneElsesBooking_forbidden() {
        assertThatThrownBy(() -> simulated().createOrder(100L, new UserPrincipal(6L, "o@mail.com", "x", "USER")))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void pay_confirmedBooking_rejected() {
        booking.setStatus(BookingStatus.CONFIRMED);

        assertThatThrownBy(() -> simulated().createOrder(100L, me)).isInstanceOf(BadRequestException.class);
    }
}
