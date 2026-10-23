package com.musicapp.service;

import com.musicapp.dto.CatalogDto.SongDto;
import com.musicapp.model.PlayHistory;
import com.musicapp.model.Song;
import com.musicapp.model.User;
import com.musicapp.repository.PlayHistoryRepository;
import com.musicapp.repository.SongRepository;
import com.musicapp.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class HistoryService {

    private final PlayHistoryRepository playHistoryRepository;
    private final SongRepository songRepository;
    private final UserRepository userRepository;
    private final SongService songService;

    public HistoryService(
        PlayHistoryRepository playHistoryRepository,
        SongRepository songRepository,
        UserRepository userRepository,
        SongService songService
    ) {
        this.playHistoryRepository = playHistoryRepository;
        this.songRepository = songRepository;
        this.userRepository = userRepository;
        this.songService = songService;
    }

    @Transactional
    public void recordPlay(Long songId, Long userId) {
        Song song = songRepository.findById(songId)
            .orElseThrow(() -> new IllegalArgumentException("Song not found"));

        song.setPlayCount(song.getPlayCount() + 1);
        songRepository.save(song);

        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId).orElse(null);
        }

        PlayHistory history = PlayHistory.builder()
            .song(song)
            .user(user)
            .build();

        playHistoryRepository.save(history);
    }

    @Transactional(readOnly = true)
    public List<SongDto> getRecentHistory(Long userId, int limit) {
        int safeLimit = Math.min(limit, 50); // Cap at 50 as specified in FR-LIB-05
        List<PlayHistory> history = playHistoryRepository.findByUserIdOrderByPlayedAtDesc(
            userId, PageRequest.of(0, safeLimit)
        );

        Set<Long> likedSongIds = songService.getLikedSongIds(userId);

        return history.stream()
            .map(h -> songService.mapToSongDto(h.getSong(), likedSongIds.contains(h.getSong().getId())))
            .collect(Collectors.toList());
    }
}
