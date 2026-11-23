package com.cinebook.dto;

/** For Razorpay all three fields come from the checkout callback; for simulated payments they can be empty. */
public record PaymentConfirmRequest(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {}
