package com.musicapp.repository;

import com.musicapp.model.Song;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SongRepository extends JpaRepository<Song, Long> {

    Optional<Song> findBySourceId(String sourceId);

    List<Song> findByTitleIgnoreCase(String title);

    boolean existsBySourceId(String sourceId);

    Page<Song> findByActiveTrue(Pageable pageable);

    Page<Song> findByGenreIgnoreCaseAndActiveTrue(String genre, Pageable pageable);

    Page<Song> findByActiveTrueOrderByPlayCountDesc(Pageable pageable);

    Page<Song> findByActiveTrueOrderByCreatedAtDesc(Pageable pageable);

    List<Song> findByArtistIdAndActiveTrue(Long artistId);

    List<Song> findByAlbumIdAndActiveTrue(Long albumId);

    List<Song> findTop10ByGenreIgnoreCaseAndIdNotAndActiveTrue(String genre, Long id);

    @Query("SELECT s FROM Song s WHERE s.active = true AND " +
           "(LOWER(s.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(s.artist.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " (s.album IS NOT NULL AND LOWER(s.album.name) LIKE LOWER(CONCAT('%', :query, '%'))))")
    Page<Song> searchActiveSongs(@Param("query") String query, Pageable pageable);

    @Query("SELECT s FROM Song s WHERE s.active = true AND LOWER(s.genre) = LOWER(:genre) AND " +
           "(LOWER(s.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(s.artist.name) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Song> searchActiveSongsByGenre(@Param("query") String query, @Param("genre") String genre, Pageable pageable);

    @Query("SELECT DISTINCT s.genre FROM Song s WHERE s.active = true AND s.genre IS NOT NULL")
    List<String> findDistinctGenres();

    @Query("SELECT s.title FROM Song s WHERE s.active = true AND LOWER(s.title) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY s.playCount DESC")
    List<String> findTopTitleSuggestions(@Param("query") String query, Pageable pageable);

    long countByActiveTrue();

    @Query("SELECT COALESCE(SUM(s.playCount), 0) FROM Song s")
    long sumTotalPlays();
}
