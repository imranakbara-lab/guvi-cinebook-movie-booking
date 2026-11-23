package com.cinebook.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BookingRequest(
        @NotNull(message = "Show is required") Long showId,
        @NotEmpty(message = "Select at least one seat") @Size(max = 10, message = "You can book at most 10 seats at a time") List<String> seats) {}
