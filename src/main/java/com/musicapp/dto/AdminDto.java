package com.musicapp.dto;

import com.musicapp.model.ImportStatus;
import lombok.*;

import java.time.LocalDateTime;

public class AdminDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AdminStatsDto {
        private long totalSongs;
        private long totalUsers;
        private long totalPlays;
        private long activeSongs;
        private long totalArtists;
        private long totalPlaylists;
        private ImportStatusDto lastImportJob;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ImportStatusDto {
        private Long jobId;
        private ImportStatus status;
        private int requested;
        private int imported;
        private int failed;
        private LocalDateTime startedAt;
        private LocalDateTime finishedAt;
        private String errorLog;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateSongStatusRequest {
        private boolean active;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImportTriggerRequest {
        private int max = 20;
        private String tag;
    }
}
