package com.musicapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class PlaylistDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Details {
        private Long id;
        private String name;
        private String title;
        private String description;
        private boolean isPublic;
        private Long userId;
        private String userName;
        private int songCount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private List<CatalogDto.SongDto> songs;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @Size(max = 100)
        private String name;

        private String title;

        @Size(max = 300)
        private String description;

        private boolean isPublic = true;

        public String getName() {
            if (name != null && !name.trim().isEmpty()) {
                return name.trim();
            }
            if (title != null && !title.trim().isEmpty()) {
                return title.trim();
            }
            return "My Playlist";
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        @NotBlank
        @Size(max = 100)
        private String name;

        @Size(max = 300)
        private String description;

        private boolean isPublic;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddSongRequest {
        private Long songId;
    }
}
