package com.musicapp.repository;

import com.musicapp.model.Album;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlbumRepository extends JpaRepository<Album, Long> {
    Optional<Album> findByJamendoId(String jamendoId);
    List<Album> findByArtistId(Long artistId);
    List<Album> findTop5ByNameContainingIgnoreCase(String name);
    Page<Album> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
