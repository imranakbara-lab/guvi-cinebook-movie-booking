package com.cinebook.dto;

import jakarta.validation.constraints.*;

public record TheaterRequest(
        @NotBlank(message = "Name is required") @Size(max = 150) String name,
        @NotBlank(message = "City is required") String city,
        @Size(max = 500) String address,
        @NotNull @Min(value = 1, message = "At least 1 row") @Max(value = 26, message = "At most 26 rows") Integer totalRows,
        @NotNull @Min(value = 1, message = "At least 1 seat per row") @Max(value = 40, message = "At most 40 seats per row") Integer seatsPerRow,
        @NotNull @Min(0) Integer premiumRows) {}
