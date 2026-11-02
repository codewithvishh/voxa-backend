package com.musicapp.dto;

import lombok.*;

import java.io.Serializable;
import java.util.List;

public class CatalogDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SongDto implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long id;
        private String sourceId;
        private String title;
        private Long artistId;
        private String artistName;
        private Long albumId;
        private String albumName;
        private String genre;
        private Integer durationSec;
        private String audioUrl;
        private String imageUrl;
        private String license;
        private Long playCount;
        private boolean active;
        private boolean isLiked;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ArtistDto implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long id;
        private String name;
        private String imageUrl;
        private String jamendoId;
        private List<SongDto> topSongs;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AlbumDto implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long id;
        private String name;
        private String imageUrl;
        private String releaseDate;
        private Long artistId;
        private String artistName;
        private List<SongDto> songs;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HomeSectionDto implements Serializable {
        private static final long serialVersionUID = 1L;
        private List<SongDto> trending;
        private List<SongDto> newReleases;
        private List<String> genres;
        private List<SongDto> recommended;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AutocompleteResultDto implements Serializable {
        private static final long serialVersionUID = 1L;
        private String query;
        private List<String> suggestions;
        private List<SongDto> songs;
        private List<ArtistDto> artists;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PageResponse<T> implements Serializable {
        private static final long serialVersionUID = 1L;
        private List<T> content;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        private boolean last;
    }
}
