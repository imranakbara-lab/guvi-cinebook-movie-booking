package com.cinebook.controller;

import com.cinebook.dto.*;
import com.cinebook.security.UserPrincipal;
import com.cinebook.service.BookingService;
import com.cinebook.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@Tag(name = "Bookings", description = "Hold seats, pay, view and cancel bookings")
public class BookingController {

    private final BookingService bookingService;
    private final PaymentService paymentService;

    public BookingController(BookingService bookingService, PaymentService paymentService) {
        this.bookingService = bookingService;
        this.paymentService = paymentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Hold seats for a show (locked for a few minutes until payment)")
    public BookingResponse hold(@Valid @RequestBody BookingRequest request, @AuthenticationPrincipal UserPrincipal me) {
        return bookingService.hold(request, me);
    }

    @PostMapping("/{id}/payment-order")
    @Operation(summary = "Create a payment order (Razorpay, or simulated when no gateway keys are set)")
    public PaymentOrderResponse paymentOrder(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal me) {
        return paymentService.createOrder(id, me);
    }

    @PostMapping("/{id}/payment-confirm")
    @Operation(summary = "Verify the payment and confirm the booking (sends confirmation email)")
    public BookingResponse confirm(@PathVariable Long id, @RequestBody(required = false) PaymentConfirmRequest request,
                                   @AuthenticationPrincipal UserPrincipal me) {
        return paymentService.confirm(id, request, me);
    }

    @GetMapping("/my")
    @Operation(summary = "My booking history")
    public List<BookingResponse> my(@AuthenticationPrincipal UserPrincipal me) { return bookingService.myBookings(me); }

    @GetMapping("/{id}")
    @Operation(summary = "Booking / ticket details")
    public BookingResponse get(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal me) {
        return bookingService.get(id, me);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a booking (releases seats, refunds if paid, sends email)")
    public BookingResponse cancel(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal me) {
        return bookingService.cancel(id, me);
    }
}
