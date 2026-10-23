package com.musicapp.controller;

import com.musicapp.dto.AuthDto.*;
import com.musicapp.dto.CatalogDto.PageResponse;
import com.musicapp.dto.CatalogDto.SongDto;
import com.musicapp.security.SecurityUtils;
import com.musicapp.service.AuthService;
import com.musicapp.service.HistoryService;
import com.musicapp.service.LikeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/me")
@Tag(name = "User Library", description = "Endpoints for user profile, liked songs, and listening history")
public class UserController {

    private final AuthService authService;
    private final LikeService likeService;
    private final HistoryService historyService;

    public UserController(
        AuthService authService,
        LikeService likeService,
        HistoryService historyService
    ) {
        this.authService = authService;
        this.likeService = likeService;
        this.historyService = historyService;
    }

    @GetMapping("/profile")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<UserProfileDto> getProfile() {
        String email = SecurityUtils.getCurrentUserEmail()
            .orElseThrow(() -> new IllegalArgumentException("User not authenticated"));
        return ResponseEntity.ok(authService.getCurrentUserProfile(email));
    }

    @PutMapping("/profile")
    @Operation(summary = "Update user profile details (name, avatar)")
    public ResponseEntity<UserProfileDto> updateProfile(@RequestBody UpdateProfileRequest request) {
        String email = SecurityUtils.getCurrentUserEmail()
            .orElseThrow(() -> new IllegalArgumentException("User not authenticated"));
        return ResponseEntity.ok(authService.updateProfile(email, request));
    }

    @PutMapping("/password")
    @Operation(summary = "Change account password")
    public ResponseEntity<Map<String, String>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        String email = SecurityUtils.getCurrentUserEmail()
            .orElseThrow(() -> new IllegalArgumentException("User not authenticated"));
        authService.changePassword(email, request);
        return ResponseEntity.ok(Collections.singletonMap("message", "Password changed successfully"));
    }

    @GetMapping("/liked")
    @Operation(summary = "Get user's liked songs library")
    public ResponseEntity<PageResponse<SongDto>> getLikedSongs(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId()
            .orElseThrow(() -> new IllegalArgumentException("User not authenticated"));
        return ResponseEntity.ok(likeService.getLikedSongs(currentUserId, page, size));
    }

    @GetMapping("/history")
    @Operation(summary = "Get user's recent listening history (up to 50 songs)")
    public ResponseEntity<List<SongDto>> getRecentHistory(
        @RequestParam(defaultValue = "50") int limit
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId()
            .orElseThrow(() -> new IllegalArgumentException("User not authenticated"));
        return ResponseEntity.ok(historyService.getRecentHistory(currentUserId, limit));
    }
}
