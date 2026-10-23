package com.musicapp.repository;

import com.musicapp.model.LikedSong;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LikedSongRepository extends JpaRepository<LikedSong, Long> {
    List<LikedSong> findByUserIdOrderByLikedAtDesc(Long userId);
    Page<LikedSong> findByUserIdOrderByLikedAtDesc(Long userId, Pageable pageable);
    Optional<LikedSong> findByUserIdAndSongId(Long userId, Long songId);
    boolean existsByUserIdAndSongId(Long userId, Long songId);
    void deleteByUserIdAndSongId(Long userId, Long songId);

    @Query("SELECT ls.song.id FROM LikedSong ls WHERE ls.user.id = :userId")
    List<Long> findLikedSongIdsByUserId(@Param("userId") Long userId);
}
