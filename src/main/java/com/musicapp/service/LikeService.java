package com.musicapp.service;

import com.musicapp.dto.CatalogDto.PageResponse;
import com.musicapp.dto.CatalogDto.SongDto;
import com.musicapp.model.LikedSong;
import com.musicapp.model.Song;
import com.musicapp.model.User;
import com.musicapp.repository.LikedSongRepository;
import com.musicapp.repository.SongRepository;
import com.musicapp.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LikeService {

    private final LikedSongRepository likedSongRepository;
    private final SongRepository songRepository;
    private final UserRepository userRepository;
    private final SongService songService;

    public LikeService(
        LikedSongRepository likedSongRepository,
        SongRepository songRepository,
        UserRepository userRepository,
        SongService songService
    ) {
        this.likedSongRepository = likedSongRepository;
        this.songRepository = songRepository;
        this.userRepository = userRepository;
        this.songService = songService;
    }

    @Transactional
    public boolean toggleLike(Long userId, Long songId) {
        if (likedSongRepository.existsByUserIdAndSongId(userId, songId)) {
            likedSongRepository.deleteByUserIdAndSongId(userId, songId);
            return false; // unliked
        } else {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
            Song song = songRepository.findById(songId)
                .orElseThrow(() -> new IllegalArgumentException("Song not found"));

            LikedSong liked = LikedSong.builder()
                .user(user)
                .song(song)
                .build();
            likedSongRepository.save(liked);
            return true; // liked
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<SongDto> getLikedSongs(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<LikedSong> likedPage = likedSongRepository.findByUserIdOrderByLikedAtDesc(userId, pageable);

        List<SongDto> songDtos = likedPage.getContent().stream()
            .map(l -> songService.mapToSongDto(l.getSong(), true))
            .collect(Collectors.toList());

        return PageResponse.<SongDto>builder()
            .content(songDtos)
            .page(likedPage.getNumber())
            .size(likedPage.getSize())
            .totalElements(likedPage.getTotalElements())
            .totalPages(likedPage.getTotalPages())
            .last(likedPage.isLast())
            .build();
    }
}
