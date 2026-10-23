package com.musicapp.repository;

import com.musicapp.model.PlayHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlayHistoryRepository extends JpaRepository<PlayHistory, Long> {
    List<PlayHistory> findByUserIdOrderByPlayedAtDesc(Long userId, Pageable pageable);

    @Query("SELECT DISTINCT ph.song.genre FROM PlayHistory ph WHERE ph.user.id = :userId AND ph.song.genre IS NOT NULL")
    List<String> findUserRecentGenres(@Param("userId") Long userId, Pageable pageable);

    long countByUserId(Long userId);
}
