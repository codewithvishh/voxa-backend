package com.musicapp.repository;

import com.musicapp.model.Artist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArtistRepository extends JpaRepository<Artist, Long> {
    Optional<Artist> findByJamendoId(String jamendoId);
    Optional<Artist> findByNameIgnoreCase(String name);
    List<Artist> findTop5ByNameContainingIgnoreCase(String name);
    Page<Artist> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
