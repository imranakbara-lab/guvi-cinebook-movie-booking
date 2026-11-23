package com.cinebook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
        @NotBlank(message = "Name is required") @Size(max = 100) String name,
        @Pattern(regexp = "^$|^[0-9+\\- ]{7,15}$", message = "Enter a valid phone number") String phone) {}
