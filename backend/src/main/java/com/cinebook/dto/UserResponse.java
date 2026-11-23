package com.cinebook.dto;

import com.cinebook.entity.Role;
import com.cinebook.entity.User;
import java.time.LocalDateTime;

public record UserResponse(Long id, String name, String email, String phone, Role role, LocalDateTime createdAt) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getPhone(), u.getRole(), u.getCreatedAt());
    }
}
