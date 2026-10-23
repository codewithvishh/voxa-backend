package com.musicapp.repository;

import com.musicapp.model.Playlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlaylistRepository extends JpaRepository<Playlist, Long> {
    List<Playlist> findByUserIdOrderByUpdatedAtDesc(Long userId);
    List<Playlist> findByIsPublicTrueOrderByCreatedAtDesc();
}
