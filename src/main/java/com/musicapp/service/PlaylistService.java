package com.musicapp.service;

import com.musicapp.dto.CatalogDto.SongDto;
import com.musicapp.dto.PlaylistDto.*;
import com.musicapp.model.Playlist;
import com.musicapp.model.PlaylistSong;
import com.musicapp.model.Song;
import com.musicapp.model.User;
import com.musicapp.repository.PlaylistRepository;
import com.musicapp.repository.PlaylistSongRepository;
import com.musicapp.repository.SongRepository;
import com.musicapp.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final PlaylistSongRepository playlistSongRepository;
    private final SongRepository songRepository;
    private final UserRepository userRepository;
    private final SongService songService;

    public PlaylistService(
        PlaylistRepository playlistRepository,
        PlaylistSongRepository playlistSongRepository,
        SongRepository songRepository,
        UserRepository userRepository,
        SongService songService
    ) {
        this.playlistRepository = playlistRepository;
        this.playlistSongRepository = playlistSongRepository;
        this.songRepository = songRepository;
        this.userRepository = userRepository;
        this.songService = songService;
    }

    @Transactional(readOnly = true)
    public List<Details> getUserPlaylists(Long userId) {
        List<Playlist> playlists = playlistRepository.findByUserIdOrderByUpdatedAtDesc(userId);
        return playlists.stream()
            .map(p -> mapToDetails(p, false, userId))
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Details> getPublicPlaylists(Long currentUserId) {
        List<Playlist> playlists = playlistRepository.findByIsPublicTrueOrderByCreatedAtDesc();
        return playlists.stream()
            .map(p -> mapToDetails(p, false, currentUserId))
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Details getPlaylistDetails(Long playlistId, Long currentUserId) {
        Playlist playlist = playlistRepository.findById(playlistId)
            .orElseThrow(() -> new IllegalArgumentException("Playlist not found"));

        if (!playlist.isPublic() && (currentUserId == null || !playlist.getUser().getId().equals(currentUserId))) {
            throw new AccessDeniedException("This playlist is private.");
        }

        return mapToDetails(playlist, true, currentUserId);
    }

    @Transactional
    public Details createPlaylist(Long userId, CreateRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Playlist playlist = Playlist.builder()
            .name(request.getName().trim())
            .description(request.getDescription())
            .isPublic(request.isPublic())
            .user(user)
            .build();

        Playlist saved = playlistRepository.save(playlist);
        return mapToDetails(saved, true, userId);
    }

    @Transactional
    public Details updatePlaylist(Long playlistId, Long userId, UpdateRequest request) {
        Playlist playlist = playlistRepository.findById(playlistId)
            .orElseThrow(() -> new IllegalArgumentException("Playlist not found"));

        if (!playlist.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You do not have permission to modify this playlist");
        }

        playlist.setName(request.getName().trim());
        playlist.setDescription(request.getDescription());
        playlist.setPublic(request.isPublic());

        Playlist saved = playlistRepository.save(playlist);
        return mapToDetails(saved, true, userId);
    }

    @Transactional
    public void deletePlaylist(Long playlistId, Long userId) {
        Playlist playlist = playlistRepository.findById(playlistId)
            .orElseThrow(() -> new IllegalArgumentException("Playlist not found"));

        if (!playlist.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You do not have permission to delete this playlist");
        }

        playlistSongRepository.deleteByPlaylistId(playlistId);
        playlistRepository.delete(playlist);
    }

    @Transactional
    public void addSongToPlaylist(Long playlistId, Long userId, Long songId) {
        Playlist playlist = playlistRepository.findById(playlistId)
            .orElseThrow(() -> new IllegalArgumentException("Playlist not found"));

        if (!playlist.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You do not have permission to modify this playlist");
        }

        if (playlistSongRepository.existsByPlaylistIdAndSongId(playlistId, songId)) {
            return; // Idempotent add
        }

        Song song = songRepository.findById(songId)
            .orElseThrow(() -> new IllegalArgumentException("Song not found"));

        int nextPos = playlistSongRepository.findMaxPositionByPlaylistId(playlistId) + 1;

        PlaylistSong item = PlaylistSong.builder()
            .playlist(playlist)
            .song(song)
            .position(nextPos)
            .build();

        playlistSongRepository.save(item);
    }

    @Transactional
    public void removeSongFromPlaylist(Long playlistId, Long userId, Long songId) {
        Playlist playlist = playlistRepository.findById(playlistId)
            .orElseThrow(() -> new IllegalArgumentException("Playlist not found"));

        if (!playlist.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You do not have permission to modify this playlist");
        }

        playlistSongRepository.deleteByPlaylistIdAndSongId(playlistId, songId);
    }

    private Details mapToDetails(Playlist p, boolean includeSongs, Long currentUserId) {
        List<PlaylistSong> items = playlistSongRepository.findByPlaylistIdOrderByPositionAsc(p.getId());
        Set<Long> likedSongIds = songService.getLikedSongIds(currentUserId);

        List<SongDto> songDtos = includeSongs ? items.stream()
            .map(item -> songService.mapToSongDto(item.getSong(), likedSongIds.contains(item.getSong().getId())))
            .collect(Collectors.toList()) : null;

        return Details.builder()
            .id(p.getId())
            .name(p.getName())
            .title(p.getName())
            .description(p.getDescription())
            .isPublic(p.isPublic())
            .userId(p.getUser().getId())
            .userName(p.getUser().getName())
            .songCount(items.size())
            .createdAt(p.getCreatedAt())
            .updatedAt(p.getUpdatedAt())
            .songs(songDtos)
            .build();
    }
}
