package com.musicapp.service;

import com.musicapp.dto.CatalogDto.*;
import com.musicapp.model.Song;
import com.musicapp.repository.AlbumRepository;
import com.musicapp.repository.ArtistRepository;
import com.musicapp.repository.LikedSongRepository;
import com.musicapp.repository.SongRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SongService {

    private final SongRepository songRepository;
    private final ArtistRepository artistRepository;
    private final AlbumRepository albumRepository;
    private final LikedSongRepository likedSongRepository;
    private final JioSaavnService jioSaavnService;

    @Transactional(readOnly = true)
    public PageResponse<SongDto> getSongs(int page, int size, String genre, Long currentUserId) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Song> songPage;

        if (genre != null && !genre.isBlank() && !genre.equalsIgnoreCase("all")) {
            songPage = songRepository.findByGenreIgnoreCaseAndActiveTrue(genre.trim(), pageable);
        } else {
            songPage = songRepository.findByActiveTrue(pageable);
        }

        Set<Long> likedSongIds = getLikedSongIds(currentUserId);

        List<SongDto> dtos = songPage.getContent().stream()
            .map(s -> mapToSongDto(s, likedSongIds.contains(s.getId())))
            .collect(Collectors.toList());

        return PageResponse.<SongDto>builder()
            .content(dtos)
            .page(songPage.getNumber())
            .size(songPage.getSize())
            .totalElements(songPage.getTotalElements())
            .totalPages(songPage.getTotalPages())
            .last(songPage.isLast())
            .build();
    }

    @Transactional(readOnly = true)
    public SongDto getSongById(Long id, Long currentUserId) {
        Song song = songRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Song not found with id: " + id));

        boolean isLiked = currentUserId != null && likedSongRepository.existsByUserIdAndSongId(currentUserId, id);
        return mapToSongDto(song, isLiked);
    }

    @Transactional
    public PageResponse<SongDto> searchSongs(String query, String genre, int page, int size, Long currentUserId) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Song> songPage;

        if (query == null || query.isBlank()) {
            return getSongs(page, size, genre, currentUserId);
        }

        String rawQuery = query.trim();
        String cleanedQuery = rawQuery
            .replaceAll("(?i)\\s*-\\s*.*$", "")
            .replaceAll("(?i)\\b(vocals?\\s*only|instrumental|slowed\\s*\\+?\\s*reverb|lo-?fi|remix)\\b", "")
            .replaceAll("\\(.*?\\)|\\[.*?\\]", "")
            .trim();
        if (cleanedQuery.isBlank()) {
            cleanedQuery = rawQuery;
        }

        if (genre != null && !genre.isBlank() && !genre.equalsIgnoreCase("all")) {
            songPage = songRepository.searchActiveSongsByGenre(cleanedQuery, genre.trim(), pageable);
        } else {
            songPage = songRepository.searchActiveSongs(cleanedQuery, pageable);
        }

        Set<Long> likedSongIds = getLikedSongIds(currentUserId);

        List<SongDto> dtos = new java.util.ArrayList<>(songPage.getContent().stream()
            .map(s -> mapToSongDto(s, likedSongIds.contains(s.getId())))
            .collect(Collectors.toList()));

        // Always query JioSaavn live catalog to fetch 30+ 320kbps full Hindi/Bollywood/global tracks
        try {
            List<SongDto> liveResults = jioSaavnService.searchAndPersist(cleanedQuery, 35, currentUserId);
            java.util.Map<String, SongDto> liveMap = new java.util.HashMap<>();
            for (SongDto liveSong : liveResults) {
                liveMap.put(liveSong.getTitle().toLowerCase().trim(), liveSong);
            }

            // Upgrade existing dtos with live 320kbps links
            for (int i = 0; i < dtos.size(); i++) {
                SongDto d = dtos.get(i);
                SongDto live = liveMap.get(d.getTitle().toLowerCase().trim());
                if (live != null && live.getAudioUrl() != null && live.getAudioUrl().contains("_320.mp4")) {
                    dtos.set(i, live);
                }
            }

            Set<String> existingTitles = dtos.stream()
                .map(d -> d.getTitle().toLowerCase().trim())
                .collect(Collectors.toSet());

            for (SongDto liveSong : liveResults) {
                if (!existingTitles.contains(liveSong.getTitle().toLowerCase().trim())) {
                    dtos.add(liveSong);
                    existingTitles.add(liveSong.getTitle().toLowerCase().trim());
                }
            }
        } catch (Exception ex) {
            // Keep local results if network error
        }

        // Prioritize official original album tracks with highest plays first
        dtos.sort((a, b) -> Long.compare(b.getPlayCount() != null ? b.getPlayCount() : 0L, a.getPlayCount() != null ? a.getPlayCount() : 0L));

        return PageResponse.<SongDto>builder()
            .content(dtos)
            .page(0)
            .size(dtos.size())
            .totalElements(dtos.size())
            .totalPages(1)
            .last(true)
            .build();
    }

    @Transactional(readOnly = true)
    public AutocompleteResultDto getAutocomplete(String query) {
        if (query == null || query.trim().length() < 2) {
            return AutocompleteResultDto.builder()
                .query(query)
                .suggestions(Collections.emptyList())
                .songs(Collections.emptyList())
                .artists(Collections.emptyList())
                .build();
        }

        String cleaned = query.trim();
        // Call JioSaavn live autocomplete API for real-time artist and song suggestions
        try {
            AutocompleteResultDto liveResult = jioSaavnService.getAutocomplete(cleaned);
            if (liveResult != null && (!liveResult.getSuggestions().isEmpty() || !liveResult.getSongs().isEmpty() || !liveResult.getArtists().isEmpty())) {
                return liveResult;
            }
        } catch (Exception ignored) {}

        // Fallback to local DB
        List<String> suggestions = songRepository.findTopTitleSuggestions(cleaned, PageRequest.of(0, 8));

        List<SongDto> matchingSongs = songRepository.searchActiveSongs(cleaned, PageRequest.of(0, 6))
            .getContent().stream()
            .map(s -> mapToSongDto(s, false))
            .collect(Collectors.toList());

        List<ArtistDto> matchingArtists = artistRepository.findTop5ByNameContainingIgnoreCase(cleaned).stream()
            .map(a -> ArtistDto.builder()
                .id(a.getId())
                .name(a.getName())
                .imageUrl(a.getImageUrl())
                .jamendoId(a.getJamendoId())
                .build())
            .collect(Collectors.toList());

        return AutocompleteResultDto.builder()
            .query(cleaned)
            .suggestions(suggestions)
            .songs(matchingSongs)
            .artists(matchingArtists)
            .build();
    }

    @Transactional(readOnly = true)
    public List<SongDto> getMoreLikeThis(Long songId, Long currentUserId) {
        Song song = songRepository.findById(songId)
            .orElseThrow(() -> new IllegalArgumentException("Song not found"));

        Set<Long> likedIds = getLikedSongIds(currentUserId);

        return songRepository.findTop10ByGenreIgnoreCaseAndIdNotAndActiveTrue(song.getGenre(), songId).stream()
            .map(s -> mapToSongDto(s, likedIds.contains(s.getId())))
            .collect(Collectors.toList());
    }

    @Transactional
    public void incrementPlayCount(Long songId) {
        songRepository.findById(songId).ifPresent(song -> {
            song.setPlayCount(song.getPlayCount() + 1);
            songRepository.save(song);
        });
    }

    public Set<Long> getLikedSongIds(Long userId) {
        if (userId == null) {
            return Collections.emptySet();
        }
        return new HashSet<>(likedSongRepository.findLikedSongIdsByUserId(userId));
    }

    public SongDto mapToSongDto(Song song, boolean isLiked) {
        return SongDto.builder()
            .id(song.getId())
            .sourceId(song.getSourceId())
            .title(song.getTitle())
            .artistId(song.getArtist().getId())
            .artistName(song.getArtist().getName())
            .albumId(song.getAlbum() != null ? song.getAlbum().getId() : null)
            .albumName(song.getAlbum() != null ? song.getAlbum().getName() : null)
            .genre(song.getGenre())
            .durationSec(song.getDurationSec())
            .audioUrl(song.getAudioUrl())
            .imageUrl(song.getImageUrl())
            .license(song.getLicense())
            .playCount(song.getPlayCount())
            .active(song.isActive())
            .isLiked(isLiked)
            .build();
    }
}
