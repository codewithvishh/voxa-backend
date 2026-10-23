package com.musicapp.controller;

import com.musicapp.dto.CatalogDto.AlbumDto;
import com.musicapp.security.SecurityUtils;
import com.musicapp.service.AlbumService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/albums")
@Tag(name = "Albums", description = "Endpoints for viewing album tracks and metadata")
public class AlbumController {

    private final AlbumService albumService;

    public AlbumController(AlbumService albumService) {
        this.albumService = albumService;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get album details and track list")
    public ResponseEntity<AlbumDto> getAlbum(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        return ResponseEntity.ok(albumService.getAlbumById(id, currentUserId));
    }
}
