package com.musicapp.service;

import com.musicapp.dto.CatalogDto.*;
import com.musicapp.model.Album;
import com.musicapp.model.Artist;
import com.musicapp.model.Song;
import com.musicapp.repository.AlbumRepository;
import com.musicapp.repository.ArtistRepository;
import com.musicapp.repository.SongRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArtistService {

    private final ArtistRepository artistRepository;
    private final SongRepository songRepository;
    private final SongService songService;

    @Transactional(readOnly = true)
    public ArtistDto getArtistById(Long id, Long currentUserId) {
        Artist artist = artistRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Artist not found with id: " + id));

        List<Song> songs = songRepository.findByArtistIdAndActiveTrue(id);
        Set<Long> likedSongIds = songService.getLikedSongIds(currentUserId);

        List<SongDto> songDtos = songs.stream()
            .map(s -> songService.mapToSongDto(s, likedSongIds.contains(s.getId())))
            .collect(Collectors.toList());

        return ArtistDto.builder()
            .id(artist.getId())
            .name(artist.getName())
            .imageUrl(artist.getImageUrl())
            .jamendoId(artist.getJamendoId())
            .topSongs(songDtos)
            .build();
    }
}
