package com.musicapp.service;

import com.musicapp.dto.CatalogDto.*;
import com.musicapp.model.Album;
import com.musicapp.model.Song;
import com.musicapp.repository.AlbumRepository;
import com.musicapp.repository.SongRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AlbumService {

    private final AlbumRepository albumRepository;
    private final SongRepository songRepository;
    private final SongService songService;

    public AlbumService(
        AlbumRepository albumRepository,
        SongRepository songRepository,
        SongService songService
    ) {
        this.albumRepository = albumRepository;
        this.songRepository = songRepository;
        this.songService = songService;
    }

    @Transactional(readOnly = true)
    public AlbumDto getAlbumById(Long id, Long currentUserId) {
        Album album = albumRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Album not found with id: " + id));

        List<Song> songs = songRepository.findByAlbumIdAndActiveTrue(id);
        Set<Long> likedSongIds = songService.getLikedSongIds(currentUserId);

        List<SongDto> songDtos = songs.stream()
            .map(s -> songService.mapToSongDto(s, likedSongIds.contains(s.getId())))
            .collect(Collectors.toList());

        return AlbumDto.builder()
            .id(album.getId())
            .name(album.getName())
            .imageUrl(album.getImageUrl())
            .releaseDate(album.getReleaseDate())
            .artistId(album.getArtist() != null ? album.getArtist().getId() : null)
            .artistName(album.getArtist() != null ? album.getArtist().getName() : null)
            .songs(songDtos)
            .build();
    }
}
