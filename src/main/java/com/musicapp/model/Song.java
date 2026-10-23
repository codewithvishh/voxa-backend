package com.musicapp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "songs",
    indexes = {
        @Index(name = "idx_song_title", columnList = "title"),
        @Index(name = "idx_song_genre", columnList = "genre"),
        @Index(name = "idx_song_play_count", columnList = "playCount"),
        @Index(name = "idx_song_source_id", columnList = "sourceId", unique = true)
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Song {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String sourceId;

    @Column(nullable = false, length = 200)
    private String title;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "artist_id", nullable = false)
    private Artist artist;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "album_id")
    private Album album;

    @Column(length = 60)
    private String genre;

    @Column(nullable = false)
    private Integer durationSec;

    @Column(nullable = false, length = 1000)
    private String audioUrl;

    @Column(length = 1000)
    private String imageUrl;

    @Column(length = 200)
    private String license;

    @Builder.Default
    @Column(nullable = false)
    private Long playCount = 0L;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Builder.Default
    @Column(nullable = false)
    private int failCount = 0;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
