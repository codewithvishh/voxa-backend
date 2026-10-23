package com.musicapp.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "albums")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Album {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 500)
    private String imageUrl;

    @Column(length = 50)
    private String releaseDate;

    @Column(unique = true, length = 100)
    private String jamendoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id")
    private Artist artist;
}
