package com.musicapp.service;

import com.musicapp.dto.CatalogDto.*;
import com.musicapp.model.Song;
import com.musicapp.repository.PlayHistoryRepository;
import com.musicapp.repository.SongRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class HomeService {

    private final SongRepository songRepository;
    private final PlayHistoryRepository playHistoryRepository;
    private final SongService songService;
    private final JioSaavnService jioSaavnService;

    public HomeService(
        SongRepository songRepository,
        PlayHistoryRepository playHistoryRepository,
        SongService songService,
        JioSaavnService jioSaavnService
    ) {
        this.songRepository = songRepository;
        this.playHistoryRepository = playHistoryRepository;
        this.songService = songService;
        this.jioSaavnService = jioSaavnService;
    }

    @Transactional
    public HomeSectionDto getHomeSections(Long currentUserId) {
        Set<Long> likedSongIds = songService.getLikedSongIds(currentUserId);

        // Trending: Top unique songs by playCount (Strict title deduplication prevents repetitive copies of the same song)
        List<Song> rawTrending = songRepository.findByActiveTrueOrderByPlayCountDesc(PageRequest.of(0, 100)).getContent();
        List<SongDto> trending = new ArrayList<>();
        Set<String> seenTrending = new java.util.HashSet<>();

        for (Song s : rawTrending) {
            String norm = normalizeTitle(s.getTitle());
            if (!norm.isEmpty() && seenTrending.add(norm)) {
                trending.add(songService.mapToSongDto(s, likedSongIds.contains(s.getId())));
            }
        }

        // If local unique songs are fewer than 35, auto-populate with distinct live hits
        if (trending.size() < 35) {
            try {
                List<SongDto> liveHits = jioSaavnService.searchAndPersist("trending hindi bollywood hits", 35, currentUserId);
                for (SongDto hit : liveHits) {
                    String norm = normalizeTitle(hit.getTitle());
                    if (!norm.isEmpty() && seenTrending.add(norm)) {
                        trending.add(hit);
                    }
                }
            } catch (Exception ignored) {}
        }

        // New Releases: Most recent unique releases
        List<Song> rawNew = songRepository.findByActiveTrueOrderByCreatedAtDesc(PageRequest.of(0, 60)).getContent();
        List<SongDto> newReleases = new ArrayList<>();
        Set<String> seenNew = new java.util.HashSet<>();

        for (Song s : rawNew) {
            String norm = normalizeTitle(s.getTitle());
            if (!norm.isEmpty() && seenNew.add(norm)) {
                newReleases.add(songService.mapToSongDto(s, likedSongIds.contains(s.getId())));
            }
        }

        if (newReleases.size() < 20) {
            try {
                List<SongDto> liveNew = jioSaavnService.searchAndPersist("latest bollywood", 25, currentUserId);
                for (SongDto hit : liveNew) {
                    String norm = normalizeTitle(hit.getTitle());
                    if (!norm.isEmpty() && seenNew.add(norm)) {
                        newReleases.add(hit);
                    }
                }
            } catch (Exception ignored) {}
        }

        // Distinct Genres
        List<String> genres = songRepository.findDistinctGenres();

        // Personalized Recommended / "Made for you"
        List<SongDto> recommended = new ArrayList<>();
        if (currentUserId != null) {
            List<String> userGenres = playHistoryRepository.findUserRecentGenres(currentUserId, PageRequest.of(0, 3));
            if (!userGenres.isEmpty()) {
                for (String g : userGenres) {
                    List<Song> songsInGenre = songRepository.findByGenreIgnoreCaseAndActiveTrue(g, PageRequest.of(0, 4)).getContent();
                    for (Song s : songsInGenre) {
                        boolean exists = false;
                        for (SongDto r : recommended) {
                            if (r.getId().equals(s.getId())) {
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) {
                            recommended.add(songService.mapToSongDto(s, likedSongIds.contains(s.getId())));
                        }
                    }
                }
            }
        }

        // Fallback for recommended if guest or no history yet
        if (recommended.isEmpty()) {
            recommended = new ArrayList<>(trending);
        }

        return HomeSectionDto.builder()
            .trending(trending)
            .newReleases(newReleases)
            .genres(genres)
            .recommended(recommended)
            .build();
    }

    private String normalizeTitle(String title) {
        if (title == null) return "";
        return title.toLowerCase()
            .replaceAll("\\(from [^)]*\\)", "")
            .replaceAll("\\([^)]*\\)", "")
            .replaceAll("\\[[^]]*\\]", "")
            .replaceAll("- .*$", "")
            .replaceAll("ft\\..*$", "")
            .replaceAll("feat\\..*$", "")
            .replaceAll("[^a-z0-9]", "")
            .trim();
    }
}
