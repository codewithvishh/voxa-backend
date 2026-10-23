package com.musicapp.controller;

import com.musicapp.dto.CatalogDto.AutocompleteResultDto;
import com.musicapp.dto.CatalogDto.PageResponse;
import com.musicapp.dto.CatalogDto.SongDto;
import com.musicapp.security.SecurityUtils;
import com.musicapp.service.SongService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "Endpoints for searching songs, artists, and live autocomplete")
public class SearchController {

    private final SongService songService;

    @GetMapping
    @Operation(summary = "Search active songs by title, artist, or album with optional genre filter")
    public ResponseEntity<PageResponse<SongDto>> search(
        @RequestParam String q,
        @RequestParam(required = false) String genre,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        return ResponseEntity.ok(songService.searchSongs(q, genre, page, size, currentUserId));
    }

    @GetMapping({"/suggest", "/suggestions"})
    @Operation(summary = "Debounced autocomplete suggestions while typing")
    public ResponseEntity<AutocompleteResultDto> suggest(@RequestParam String q) {
        return ResponseEntity.ok(songService.getAutocomplete(q));
    }
}
