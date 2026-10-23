package com.musicapp.controller;

import com.musicapp.dto.CatalogDto.PageResponse;
import com.musicapp.dto.CatalogDto.SongDto;
import com.musicapp.security.SecurityUtils;
import com.musicapp.service.HistoryService;
import com.musicapp.service.LikeService;
import com.musicapp.service.SongService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/songs")
@Tag(name = "Songs", description = "Endpoints for discovering, playing, and liking songs")
public class SongController {

    private final SongService songService;
    private final LikeService likeService;
    private final HistoryService historyService;

    public SongController(
        SongService songService,
        LikeService likeService,
        HistoryService historyService
    ) {
        this.songService = songService;
        this.likeService = likeService;
        this.historyService = historyService;
    }

    @GetMapping
    @Operation(summary = "Get paginated songs catalog, optionally filtered by genre")
    public ResponseEntity<PageResponse<SongDto>> getSongs(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String genre
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        return ResponseEntity.ok(songService.getSongs(page, size, genre, currentUserId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get detailed metadata for a specific song")
    public ResponseEntity<SongDto> getSong(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        return ResponseEntity.ok(songService.getSongById(id, currentUserId));
    }

    @PostMapping("/{id}/play")
    @Operation(summary = "Record play event after 30 seconds of playback and increment count")
    public ResponseEntity<Map<String, String>> recordPlay(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        historyService.recordPlay(id, currentUserId);
        return ResponseEntity.ok(Collections.singletonMap("message", "Play recorded"));
    }

    @PostMapping("/{id}/like")
    @Operation(summary = "Toggle like/unlike for a song (requires authenticated user)")
    public ResponseEntity<Map<String, Object>> toggleLike(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId()
            .orElseThrow(() -> new IllegalArgumentException("User must be logged in to like a song"));
        boolean liked = likeService.toggleLike(currentUserId, id);
        return ResponseEntity.ok(Map.of("songId", id, "liked", liked));
    }

    @GetMapping("/{id}/similar")
    @Operation(summary = "Get recommended similar songs ('More like this')")
    public ResponseEntity<List<SongDto>> getSimilarSongs(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        return ResponseEntity.ok(songService.getMoreLikeThis(id, currentUserId));
    }
}
