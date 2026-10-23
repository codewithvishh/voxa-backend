package com.musicapp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "liked_songs",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_song_like", columnNames = {"user_id", "song_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LikedSong {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "song_id", nullable = false)
    private Song song;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime likedAt;
}
