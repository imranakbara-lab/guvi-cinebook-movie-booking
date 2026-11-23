package com.cinebook.dto;

/**
 * Data the browser needs to collect payment.
 * provider = RAZORPAY -> open Razorpay Checkout with keyId + orderId.
 * provider = SIMULATED -> show the test payment form and call /confirm.
 */
public record PaymentOrderResponse(Long bookingId, String provider, String keyId, String orderId,
                                   long amountInPaise, String currency, String customerName, String customerEmail,
                                   String description) {}
