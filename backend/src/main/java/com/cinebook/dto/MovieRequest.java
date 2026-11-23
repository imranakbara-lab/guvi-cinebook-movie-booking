package com.cinebook.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record MovieRequest(
        @NotBlank(message = "Title is required") @Size(max = 200) String title,
        @Size(max = 3000) String description,
        @NotBlank(message = "Genre is required") String genre,
        String language,
        @NotNull(message = "Duration is required") @Min(value = 30, message = "Duration must be at least 30 minutes")
        @Max(value = 300, message = "Duration cannot exceed 300 minutes") Integer durationMinutes,
        @Pattern(regexp = "^$|^(U|UA|U/A|A|S|UA7\\+|UA13\\+|UA16\\+)$", message = "Certificate must be U, UA or A") String certificate,
        @DecimalMin(value = "0.0") @DecimalMax(value = "10.0", message = "Rating must be between 0 and 10") Double rating,
        @Size(max = 1000) String posterUrl,
        LocalDate releaseDate,
        boolean active) {}
