package com.musicapp.repository;

import com.musicapp.model.PlaylistSong;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlaylistSongRepository extends JpaRepository<PlaylistSong, Long> {
    List<PlaylistSong> findByPlaylistIdOrderByPositionAsc(Long playlistId);
    Optional<PlaylistSong> findByPlaylistIdAndSongId(Long playlistId, Long songId);
    void deleteByPlaylistIdAndSongId(Long playlistId, Long songId);
    void deleteByPlaylistId(Long playlistId);
    boolean existsByPlaylistIdAndSongId(Long playlistId, Long songId);

    @Query("SELECT COALESCE(MAX(ps.position), 0) FROM PlaylistSong ps WHERE ps.playlist.id = :playlistId")
    int findMaxPositionByPlaylistId(@Param("playlistId") Long playlistId);
}
