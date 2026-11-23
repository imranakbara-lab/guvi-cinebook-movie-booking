package com.cinebook.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ShowRequest(
        @NotNull(message = "Movie is required") Long movieId,
        @NotNull(message = "Theater is required") Long theaterId,
        @NotNull(message = "Start time is required") @Future(message = "Start time must be in the future") LocalDateTime startTime,
        @NotNull(message = "Price is required") @Positive(message = "Price must be greater than 0") BigDecimal price,
        @Positive(message = "Premium price must be greater than 0") BigDecimal premiumPrice) {}
