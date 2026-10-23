package com.musicapp.bootstrap;

import com.musicapp.model.*;
import com.musicapp.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ArtistRepository artistRepository;
    private final AlbumRepository albumRepository;
    private final SongRepository songRepository;
    private final PlaylistRepository playlistRepository;
    private final PlaylistSongRepository playlistSongRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        seedUsers();
        seedMusicCatalog();
        seedDemoPlaylist();
    }

    private void seedUsers() {
        if (!userRepository.existsByEmail("admin@musicapp.com")) {
            User admin = User.builder()
                .name("System Administrator")
                .email("admin@musicapp.com")
                .passwordHash(passwordEncoder.encode("admin123"))
                .role(Role.ROLE_ADMIN)
                .avatarUrl("https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150")
                .active(true)
                .build();
            userRepository.save(admin);
            log.info("Initialized default ADMIN account: admin@musicapp.com / admin123");
        }

        if (!userRepository.existsByEmail("user@musicapp.com")) {
            User demoUser = User.builder()
                .name("Alex Rivers")
                .email("user@musicapp.com")
                .passwordHash(passwordEncoder.encode("user123"))
                .role(Role.ROLE_USER)
                .avatarUrl("https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150")
                .active(true)
                .build();
            userRepository.save(demoUser);
            log.info("Initialized default USER account: user@musicapp.com / user123");
        }
    }

    private void seedMusicCatalog() {
        if (songRepository.count() > 0) {
            return;
        }

        log.info("Seeding initial music catalog with Creative Commons streaming tracks...");

        // Artist 1: Neon Echoes (Synthwave / Electronic)
        Artist a1 = artistRepository.save(Artist.builder()
            .name("Neon Echoes")
            .jamendoId("art_101")
            .imageUrl("https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=500")
            .build());

        Album alb1 = albumRepository.save(Album.builder()
            .name("Cybernetic Sunset")
            .jamendoId("alb_101")
            .artist(a1)
            .imageUrl("https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=500")
            .releaseDate("2024-03-15")
            .build());

        // Artist 2: Luna Mirage (Lo-Fi / Chill)
        Artist a2 = artistRepository.save(Artist.builder()
            .name("Luna Mirage")
            .jamendoId("art_102")
            .imageUrl("https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=500")
            .build());

        Album alb2 = albumRepository.save(Album.builder()
            .name("Coffee & Raindrops")
            .jamendoId("alb_102")
            .artist(a2)
            .imageUrl("https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=500")
            .releaseDate("2023-11-20")
            .build());

        // Artist 3: The Velvet Bloom (Indie Pop / Rock)
        Artist a3 = artistRepository.save(Artist.builder()
            .name("The Velvet Bloom")
            .jamendoId("art_103")
            .imageUrl("https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=500")
            .build());

        Album alb3 = albumRepository.save(Album.builder()
            .name("Golden Horizons")
            .jamendoId("alb_103")
            .artist(a3)
            .imageUrl("https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=500")
            .releaseDate("2024-01-10")
            .build());

        // Artist 4: Apex Pulse (EDM / Dance)
        Artist a4 = artistRepository.save(Artist.builder()
            .name("Apex Pulse")
            .jamendoId("art_104")
            .imageUrl("https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=500")
            .build());

        Album alb4 = albumRepository.save(Album.builder()
            .name("Overdrive Velocity")
            .jamendoId("alb_104")
            .artist(a4)
            .imageUrl("https://images.unsplash.com/photo-1470229722913-7c0e2dbbafd3?w=500")
            .releaseDate("2024-04-05")
            .build());

        // Artist 5: Quiet Reverie (Acoustic / Folk)
        Artist a5 = artistRepository.save(Artist.builder()
            .name("Quiet Reverie")
            .jamendoId("art_105")
            .imageUrl("https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500")
            .build());

        Album alb5 = albumRepository.save(Album.builder()
            .name("Whispering Woods")
            .jamendoId("alb_105")
            .artist(a5)
            .imageUrl("https://images.unsplash.com/photo-1447752875215-b2761acb3c5d?w=500")
            .releaseDate("2023-09-12")
            .build());

        // Streamable Creative Commons / Royalty Free Audio Streams
        // Using high reliability audio test streams & Creative Commons MP3s
        Object[][] songs = {
            {"jam_1001", "Midnight City Lights", a1, alb1, "Synthwave", 215, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3", "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=500", 1420L},
            {"jam_1002", "Retro Highway 84", a1, alb1, "Synthwave", 188, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3", "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=500", 980L},
            {"jam_1003", "Neon Dreams", a1, alb1, "Electronic", 230, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3", "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=500", 870L},
            {"jam_1004", "Sunday Morning Brew", a2, alb2, "Chillout", 160, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3", "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=500", 2150L},
            {"jam_1005", "Tokyo Street Drizzle", a2, alb2, "Chillout", 175, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=500", 1890L},
            {"jam_1006", "Cozy Fireside", a2, alb2, "Ambient", 195, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3", "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500", 640L},
            {"jam_1007", "Wildflowers & Gold", a3, alb3, "Indie", 205, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3", "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=500", 3120L},
            {"jam_1008", "Chasing Summer Wind", a3, alb3, "Rock", 190, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3", "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=500", 2430L},
            {"jam_1009", "Electric Euphoria", a4, alb4, "Dance", 220, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3", "https://images.unsplash.com/photo-1470229722913-7c0e2dbbafd3?w=500", 1780L},
            {"jam_1010", "Subzero Bass", a4, alb4, "Electronic", 210, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-10.mp3", "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=500", 1340L},
            {"jam_1011", "Mountain Pine Whispers", a5, alb5, "Acoustic", 168, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-11.mp3", "https://images.unsplash.com/photo-1447752875215-b2761acb3c5d?w=500", 1520L},
            {"jam_1012", "Canyon Echoes", a5, alb5, "Folk", 198, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-12.mp3", "https://images.unsplash.com/photo-1441974231531-c6227db76b6e?w=500", 890L}
        };

        for (Object[] s : songs) {
            songRepository.save(Song.builder()
                .sourceId((String) s[0])
                .title((String) s[1])
                .artist((Artist) s[2])
                .album((Album) s[3])
                .genre((String) s[4])
                .durationSec((Integer) s[5])
                .audioUrl((String) s[6])
                .imageUrl((String) s[7])
                .license("Creative Commons Attribution-NonCommercial (CC BY-NC)")
                .playCount((Long) s[8])
                .active(true)
                .failCount(0)
                .build());
        }

        log.info("Successfully seeded {} songs across 5 artists and albums.", songs.length);
    }

    private void seedDemoPlaylist() {
        if (playlistRepository.count() > 0) return;

        userRepository.findByEmail("user@musicapp.com").ifPresent(user -> {
            Playlist playlist = playlistRepository.save(Playlist.builder()
                .name("Late Night Chill & Focus")
                .description("Curated ambient beats and relaxing synth melodies for studying and relaxing.")
                .isPublic(true)
                .user(user)
                .build());

            List<Song> allSongs = songRepository.findAll();
            for (int i = 0; i < Math.min(5, allSongs.size()); i++) {
                playlistSongRepository.save(PlaylistSong.builder()
                    .playlist(playlist)
                    .song(allSongs.get(i))
                    .position(i + 1)
                    .build());
            }

            log.info("Seeded initial starter playlist for demo user.");
        });
    }
}
