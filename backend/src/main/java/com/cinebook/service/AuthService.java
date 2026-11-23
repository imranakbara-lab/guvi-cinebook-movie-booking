package com.cinebook.service;

import com.cinebook.dto.AuthResponse;
import com.cinebook.dto.LoginRequest;
import com.cinebook.dto.RegisterRequest;
import com.cinebook.dto.UserResponse;
import com.cinebook.entity.Role;
import com.cinebook.entity.User;
import com.cinebook.exception.BadRequestException;
import com.cinebook.repository.UserRepository;
import com.cinebook.security.JwtService;
import com.cinebook.security.UserPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
                       AuthenticationManager authenticationManager, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.emailService = emailService;
    }

    /** Public sign-up always creates a USER. Admins are created by the seeder or promoted by an admin. */
    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) throw new BadRequestException("Email is already registered");
        User saved = userRepository.save(new User(req.name().trim(), email, passwordEncoder.encode(req.password()),
                req.phone() == null || req.phone().isBlank() ? null : req.phone().trim(), Role.USER));
        emailService.sendWelcome(saved.getEmail(), saved.getName());
        return new AuthResponse(jwtService.generateToken(UserPrincipal.from(saved)), UserResponse.from(saved));
    }

    public AuthResponse login(LoginRequest req) {
        String email = req.email().trim().toLowerCase();
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, req.password()));
        User user = userRepository.findByEmail(email).orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        return new AuthResponse(jwtService.generateToken(UserPrincipal.from(user)), UserResponse.from(user));
    }
}
