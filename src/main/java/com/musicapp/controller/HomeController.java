package com.musicapp.controller;

import com.musicapp.dto.CatalogDto.HomeSectionDto;
import com.musicapp.security.SecurityUtils;
import com.musicapp.service.HomeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/home")
@Tag(name = "Home", description = "Endpoints for home landing page feeds (Trending, New, Genres, Recommendations)")
public class HomeController {

    private final HomeService homeService;

    public HomeController(HomeService homeService) {
        this.homeService = homeService;
    }

    @GetMapping
    @Operation(summary = "Get home discovery sections")
    public ResponseEntity<HomeSectionDto> getHomeFeed() {
        Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        return ResponseEntity.ok(homeService.getHomeSections(currentUserId));
    }
}
