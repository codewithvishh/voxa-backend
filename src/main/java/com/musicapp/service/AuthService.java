package com.musicapp.service;

import com.musicapp.dto.AuthDto.*;
import com.musicapp.model.Role;
import com.musicapp.model.User;
import com.musicapp.repository.UserRepository;
import com.musicapp.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    public OtpStatusResponse sendRegistrationOtp(SendOtpRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("An account with email " + email + " already exists. Please log in.");
        }

        emailService.generateAndSendOtp(email, request.getName());

        return OtpStatusResponse.builder()
            .success(true)
            .message("Verification One Time Password (OTP) has been sent to " + email)
            .email(email)
            .build();
    }

    @Transactional
    public AuthResponse verifyOtpAndRegister(VerifyOtpRegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("An account with email " + email + " already exists. Please log in.");
        }

        boolean isValid = emailService.verifyOtp(email, request.getOtp());
        if (!isValid) {
            throw new IllegalArgumentException("Invalid or expired One Time Password (OTP). Please check your email or request a new code.");
        }

        User user = User.builder()
            .name(request.getName().trim())
            .email(email)
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .role(Role.ROLE_USER)
            .active(true)
            .avatarUrl("https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150")
            .build();

        User savedUser = userRepository.save(user);

        String accessToken = tokenProvider.generateAccessToken(savedUser.getEmail(), savedUser.getRole().name(), savedUser.getId());
        String refreshToken = tokenProvider.generateRefreshToken(savedUser.getEmail());

        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .user(mapToProfileDto(savedUser))
            .build();
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already registered: " + email);
        }

        User user = User.builder()
            .name(request.getName().trim())
            .email(email)
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .role(Role.ROLE_USER)
            .active(true)
            .avatarUrl("https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150")
            .build();

        User savedUser = userRepository.save(user);

        String accessToken = tokenProvider.generateAccessToken(savedUser.getEmail(), savedUser.getRole().name(), savedUser.getId());
        String refreshToken = tokenProvider.generateRefreshToken(savedUser.getEmail());

        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .user(mapToProfileDto(savedUser))
            .build();
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(email, request.getPassword())
        );

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));

        if (!user.isActive()) {
            throw new IllegalStateException("Your account has been deactivated. Please contact support.");
        }

        String accessToken = tokenProvider.generateAccessToken(user.getEmail(), user.getRole().name(), user.getId());
        String refreshToken = tokenProvider.generateRefreshToken(user.getEmail());

        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .user(mapToProfileDto(user))
            .build();
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }

        String email = tokenProvider.getEmailFromToken(refreshToken);
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));

        if (!user.isActive()) {
            throw new IllegalStateException("Your account has been deactivated.");
        }

        String newAccessToken = tokenProvider.generateAccessToken(user.getEmail(), user.getRole().name(), user.getId());
        String newRefreshToken = tokenProvider.generateRefreshToken(user.getEmail());

        return AuthResponse.builder()
            .accessToken(newAccessToken)
            .refreshToken(newRefreshToken)
            .tokenType("Bearer")
            .user(mapToProfileDto(user))
            .build();
    }

    @Transactional(readOnly = true)
    public UserProfileDto getCurrentUserProfile(String email) {
        User user = userRepository.findByEmail(email.toLowerCase())
            .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
        return mapToProfileDto(user);
    }

    @Transactional
    public UserProfileDto updateProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email.toLowerCase())
            .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));

        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName().trim());
        }
        if (request.getAvatarUrl() != null && !request.getAvatarUrl().isBlank()) {
            user.setAvatarUrl(request.getAvatarUrl().trim());
        }

        return mapToProfileDto(userRepository.save(user));
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email.toLowerCase())
            .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password does not match");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    public UserProfileDto mapToProfileDto(User user) {
        return UserProfileDto.builder()
            .id(user.getId())
            .name(user.getName())
            .email(user.getEmail())
            .role(user.getRole())
            .avatarUrl(user.getAvatarUrl())
            .createdAt(user.getCreatedAt())
            .build();
    }
}
