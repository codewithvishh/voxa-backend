package com.musicapp.controller;

import com.musicapp.dto.PlaylistDto.*;
import com.musicapp.security.SecurityUtils;
import com.musicapp.service.PlaylistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/playlists")
@Tag(name = "Playlists", description = "Endpoints for managing user and public playlists")
public class PlaylistController {

    private final PlaylistService playlistService;

    public PlaylistController(PlaylistService playlistService) {
        this.playlistService = playlistService;
    }

    @GetMapping
    @Operation(summary = "Get current authenticated user's playlists")
    public ResponseEntity<List<Details>> getMyPlaylists() {
        Long currentUserId = SecurityUtils.getCurrentUserId()
            .orElseThrow(() -> new IllegalArgumentException("User is not authenticated"));
        return ResponseEntity.ok(playlistService.getUserPlaylists(currentUserId));
    }

    @GetMapping("/public")
    @Operation(summary = "Get community public playlists")
    public ResponseEntity<List<Details>> getPublicPlaylists() {
        Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        return ResponseEntity.ok(playlistService.getPublicPlaylists(currentUserId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get playlist details with all songs")
    public ResponseEntity<Details> getPlaylist(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        return ResponseEntity.ok(playlistService.getPlaylistDetails(id, currentUserId));
    }

    @PostMapping
    @Operation(summary = "Create a new playlist")
    public ResponseEntity<Details> createPlaylist(@Valid @RequestBody CreateRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId()
            .orElseThrow(() -> new IllegalArgumentException("User is not authenticated"));
        return ResponseEntity.ok(playlistService.createPlaylist(currentUserId, request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update playlist details (name, description, visibility)")
    public ResponseEntity<Details> updatePlaylist(
        @PathVariable Long id,
        @Valid @RequestBody UpdateRequest request
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId()
            .orElseThrow(() -> new IllegalArgumentException("User is not authenticated"));
        return ResponseEntity.ok(playlistService.updatePlaylist(id, currentUserId, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a playlist")
    public ResponseEntity<Map<String, String>> deletePlaylist(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId()
            .orElseThrow(() -> new IllegalArgumentException("User is not authenticated"));
        playlistService.deletePlaylist(id, currentUserId);
        return ResponseEntity.ok(Collections.singletonMap("message", "Playlist deleted successfully"));
    }

    @PostMapping("/{id}/songs")
    @Operation(summary = "Add a song to playlist")
    public ResponseEntity<Map<String, String>> addSong(
        @PathVariable Long id,
        @RequestBody AddSongRequest request
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId()
            .orElseThrow(() -> new IllegalArgumentException("User is not authenticated"));
        playlistService.addSongToPlaylist(id, currentUserId, request.getSongId());
        return ResponseEntity.ok(Collections.singletonMap("message", "Song added to playlist"));
    }

    @DeleteMapping("/{id}/songs/{songId}")
    @Operation(summary = "Remove a song from playlist")
    public ResponseEntity<Map<String, String>> removeSong(
        @PathVariable Long id,
        @PathVariable Long songId
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId()
            .orElseThrow(() -> new IllegalArgumentException("User is not authenticated"));
        playlistService.removeSongFromPlaylist(id, currentUserId, songId);
        return ResponseEntity.ok(Collections.singletonMap("message", "Song removed from playlist"));
    }
}
