package com.musicapp.controller;

import com.musicapp.dto.CatalogDto.ArtistDto;
import com.musicapp.security.SecurityUtils;
import com.musicapp.service.ArtistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/artists")
@Tag(name = "Artists", description = "Endpoints for viewing artist profiles and discographies")
public class ArtistController {

    private final ArtistService artistService;

    public ArtistController(ArtistService artistService) {
        this.artistService = artistService;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get artist details with their song list")
    public ResponseEntity<ArtistDto> getArtist(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        return ResponseEntity.ok(artistService.getArtistById(id, currentUserId));
    }
}
