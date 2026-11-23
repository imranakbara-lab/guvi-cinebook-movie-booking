package com.cinebook.service;

import com.cinebook.dto.*;
import com.cinebook.entity.User;
import com.cinebook.exception.BadRequestException;
import com.cinebook.exception.ResourceNotFoundException;
import com.cinebook.repository.BookingRepository;
import com.cinebook.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** User module: profile management and admin user management. */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, BookingRepository bookingRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return UserResponse.from(find(id));
    }

    @Transactional
    public UserResponse updateProfile(Long id, ProfileUpdateRequest req) {
        User u = find(id);
        u.setName(req.name().trim());
        u.setPhone(req.phone() == null || req.phone().isBlank() ? null : req.phone().trim());
        return UserResponse.from(userRepository.save(u));
    }

    @Transactional
    public MessageResponse changePassword(Long id, ChangePasswordRequest req) {
        User u = find(id);
        if (!passwordEncoder.matches(req.currentPassword(), u.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }
        u.setPassword(passwordEncoder.encode(req.newPassword()));
        userRepository.save(u);
        return new MessageResponse("Password changed successfully");
    }

    // ---------- Admin ----------

    @Transactional(readOnly = true)
    public List<UserResponse> listAll() {
        return userRepository.findAll().stream().map(UserResponse::from).toList();
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest req) {
        User user = find(id);
        String email = req.email().trim().toLowerCase();
        userRepository.findByEmail(email).filter(o -> !o.getId().equals(id))
                .ifPresent(o -> { throw new BadRequestException("Email is already used by another user"); });
        user.setName(req.name().trim());
        user.setEmail(email);
        user.setPhone(req.phone() == null || req.phone().isBlank() ? null : req.phone().trim());
        user.setRole(req.role());
        return UserResponse.from(userRepository.save(user));
    }

    /** Deletes the user and their bookings (releasing any seats). An admin cannot delete their own account. */
    @Transactional
    public void delete(Long id, Long currentAdminId) {
        if (id.equals(currentAdminId)) throw new BadRequestException("You cannot delete your own account");
        User user = find(id);
        bookingRepository.deleteByUserId(user.getId());
        userRepository.delete(user);
    }

    private User find(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found with id " + id));
    }
}
