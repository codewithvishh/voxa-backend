package com.musicapp.controller;

import com.musicapp.dto.AuthDto.*;
import com.musicapp.security.SecurityUtils;
import com.musicapp.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Endpoints for user registration, login, and token refresh")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/send-otp")
    @Operation(summary = "Send email verification OTP for registration")
    public ResponseEntity<OtpStatusResponse> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        return ResponseEntity.ok(authService.sendRegistrationOtp(request));
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify OTP and create new user account")
    public ResponseEntity<AuthResponse> verifyOtpAndRegister(@Valid @RequestBody VerifyOtpRegisterRequest request) {
        return ResponseEntity.ok(authService.verifyOtpAndRegister(request));
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and return JWT access and refresh tokens")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh JWT access token using a valid refresh token")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile")
    public ResponseEntity<UserProfileDto> getProfile() {
        String email = SecurityUtils.getCurrentUserEmail()
            .orElseThrow(() -> new IllegalArgumentException("User is not authenticated"));
        return ResponseEntity.ok(authService.getCurrentUserProfile(email));
    }
}
