package com.cinebook.entity;

/** PENDING_PAYMENT seats are held for a few minutes; unpaid holds become EXPIRED. */
public enum BookingStatus { PENDING_PAYMENT, CONFIRMED, CANCELLED, EXPIRED }
