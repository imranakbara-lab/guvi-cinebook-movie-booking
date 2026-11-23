package com.cinebook.controller;

import com.cinebook.dto.*;
import com.cinebook.security.UserPrincipal;
import com.cinebook.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me")
@Tag(name = "Profile", description = "View and update your profile")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) { this.userService = userService; }

    @GetMapping
    @Operation(summary = "My profile")
    public UserResponse me(@AuthenticationPrincipal UserPrincipal me) { return userService.getById(me.getId()); }

    @PutMapping
    @Operation(summary = "Update name and phone")
    public UserResponse update(@AuthenticationPrincipal UserPrincipal me, @Valid @RequestBody ProfileUpdateRequest request) {
        return userService.updateProfile(me.getId(), request);
    }

    @PutMapping("/password")
    @Operation(summary = "Change password")
    public MessageResponse changePassword(@AuthenticationPrincipal UserPrincipal me, @Valid @RequestBody ChangePasswordRequest request) {
        return userService.changePassword(me.getId(), request);
    }
}
