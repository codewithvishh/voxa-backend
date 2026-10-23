package com.musicapp.service;

import com.musicapp.dto.AdminDto.AdminStatsDto;
import com.musicapp.dto.AdminDto.ImportStatusDto;
import com.musicapp.dto.AuthDto.UserProfileDto;
import com.musicapp.model.ImportJob;
import com.musicapp.model.Song;
import com.musicapp.model.User;
import com.musicapp.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final SongRepository songRepository;
    private final ArtistRepository artistRepository;
    private final PlaylistRepository playlistRepository;
    private final ImportJobRepository importJobRepository;
    private final AuthService authService;

    public AdminService(
        UserRepository userRepository,
        SongRepository songRepository,
        ArtistRepository artistRepository,
        PlaylistRepository playlistRepository,
        ImportJobRepository importJobRepository,
        AuthService authService
    ) {
        this.userRepository = userRepository;
        this.songRepository = songRepository;
        this.artistRepository = artistRepository;
        this.playlistRepository = playlistRepository;
        this.importJobRepository = importJobRepository;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public AdminStatsDto getStats() {
        long totalSongs = songRepository.count();
        long activeSongs = songRepository.countByActiveTrue();
        long totalUsers = userRepository.count();
        long totalPlays = songRepository.sumTotalPlays();
        long totalArtists = artistRepository.count();
        long totalPlaylists = playlistRepository.count();

        List<ImportJob> recentJobs = importJobRepository.findAllByOrderByStartedAtDesc();
        ImportStatusDto lastJob = null;
        if (!recentJobs.isEmpty()) {
            ImportJob j = recentJobs.get(0);
            lastJob = ImportStatusDto.builder()
                .jobId(j.getId())
                .status(j.getStatus())
                .requested(j.getRequested())
                .imported(j.getImported())
                .failed(j.getFailed())
                .startedAt(j.getStartedAt())
                .finishedAt(j.getFinishedAt())
                .errorLog(j.getErrorLog())
                .build();
        }

        return AdminStatsDto.builder()
            .totalSongs(totalSongs)
            .activeSongs(activeSongs)
            .totalUsers(totalUsers)
            .totalPlays(totalPlays)
            .totalArtists(totalArtists)
            .totalPlaylists(totalPlaylists)
            .lastImportJob(lastJob)
            .build();
    }

    @Transactional(readOnly = true)
    public Page<UserProfileDto> getAllUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return userRepository.findAll(pageable).map(authService::mapToProfileDto);
    }

    @Transactional
    public void toggleUserStatus(Long userId, boolean active) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        user.setActive(active);
        userRepository.save(user);
    }

    @Transactional
    public void setSongActiveStatus(Long songId, boolean active) {
        Song song = songRepository.findById(songId)
            .orElseThrow(() -> new IllegalArgumentException("Song not found with id: " + songId));
        song.setActive(active);
        songRepository.save(song);
    }
}
