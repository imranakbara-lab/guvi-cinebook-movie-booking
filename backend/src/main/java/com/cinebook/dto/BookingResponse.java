package com.cinebook.dto;

import com.cinebook.entity.Booking;
import com.cinebook.entity.BookingStatus;
import com.cinebook.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public record BookingResponse(Long id, String reference, BookingStatus status, PaymentStatus paymentStatus,
                              String paymentProvider, String paymentId, List<String> seats, BigDecimal amount,
                              LocalDateTime createdAt, LocalDateTime holdExpiresAt, long holdSecondsLeft,
                              LocalDateTime confirmedAt, LocalDateTime cancelledAt, boolean cancellable,
                              String userName, String userEmail, ShowResponse show) {

    public static BookingResponse from(Booking b, LocalDateTime now, boolean cancellable) {
        long left = b.getStatus() == BookingStatus.PENDING_PAYMENT
                ? Math.max(0, Duration.between(now, b.getHoldExpiresAt()).getSeconds()) : 0;
        return new BookingResponse(b.getId(), b.getReference(), b.getStatus(), b.getPaymentStatus(),
                b.getPaymentProvider(), b.getPaymentId(), b.seats(), b.getAmount(), b.getCreatedAt(),
                b.getHoldExpiresAt(), left, b.getConfirmedAt(), b.getCancelledAt(), cancellable,
                b.getUser().getName(), b.getUser().getEmail(), ShowResponse.from(b.getShow(), 0));
    }
}
