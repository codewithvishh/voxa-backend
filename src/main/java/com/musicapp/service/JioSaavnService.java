package com.musicapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.musicapp.dto.CatalogDto.SongDto;
import com.musicapp.model.Album;
import com.musicapp.model.Artist;
import com.musicapp.model.Song;
import com.musicapp.repository.AlbumRepository;
import com.musicapp.repository.ArtistRepository;
import com.musicapp.repository.LikedSongRepository;
import com.musicapp.repository.SongRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class JioSaavnService {

    private static final byte[] DES_KEY = "38346591".getBytes(StandardCharsets.UTF_8);

    private final SongRepository songRepository;
    private final ArtistRepository artistRepository;
    private final AlbumRepository albumRepository;
    private final LikedSongRepository likedSongRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(6))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

    /**
     * Decrypts JioSaavn's encrypted_media_url into the high-bitrate 320kbps CD/studio quality CDN stream.
     */
    public static String decryptMediaUrl(String encryptedUrl) {
        if (encryptedUrl == null || encryptedUrl.isBlank()) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(DES_KEY, "DES");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decoded = Base64.getDecoder().decode(encryptedUrl.trim());
            byte[] decrypted = cipher.doFinal(decoded);
            String rawUrl = new String(decrypted, StandardCharsets.UTF_8).trim();
            // Upgrade to 320kbps master studio quality stream
            return rawUrl.replace("_96.mp4", "_320.mp4").replace("_160.mp4", "_320.mp4");
        } catch (Exception e) {
            log.warn("Failed to decrypt JioSaavn media url: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Search global & Indian catalog (JioSaavn) across multiple pages, deduplicate,
     * auto-persist new songs to PostgreSQL, and return unified SongDto list (30+ tracks).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<SongDto> searchAndPersist(String query, int limit, Long currentUserId) {
        List<SongDto> resultList = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            return resultList;
        }

        java.util.Set<String> seenNormalizedTitles = new java.util.HashSet<>();
        int targetCount = Math.max(limit, 30);
        int maxPages = 3;

        try {
            String encodedQuery = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);

            for (int page = 1; page <= maxPages && resultList.size() < targetCount; page++) {
                try {
                    String url = String.format(
                        "https://www.jiosaavn.com/api.php?__call=search.getResults&_format=json&_marker=0&api_version=4&ctx=web6dot0&n=35&p=%d&q=%s",
                        page,
                        encodedQuery
                    );

                    HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .header("Accept", "application/json, text/plain, */*")
                        .timeout(Duration.ofSeconds(6))
                        .GET()
                        .build();

                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() == 200 && response.body() != null) {
                        JsonNode root = objectMapper.readTree(response.body());
                        JsonNode results = root.path("results");

                        if (results.isArray()) {
                            // Collect and group results to prefer authentic original film soundtracks over compilation albums
                            java.util.Map<String, JsonNode> bestByNorm = new java.util.LinkedHashMap<>();
                            for (JsonNode item : results) {
                                String rawTitle = cleanHtml(item.path("title").asText("Unknown Title"));
                                String norm = rawTitle.toLowerCase().replaceAll("[^a-z0-9]", "");
                                if (norm.isBlank()) continue;

                                if (!bestByNorm.containsKey(norm)) {
                                    bestByNorm.put(norm, item);
                                } else {
                                    JsonNode prev = bestByNorm.get(norm);
                                    int prevScore = calculateOriginalScore(prev);
                                    int newScore = calculateOriginalScore(item);
                                    long prevPlays = 0;
                                    long newPlays = 0;
                                    try { prevPlays = Long.parseLong(prev.path("play_count").asText("0")); } catch (Exception ignored) {}
                                    try { newPlays = Long.parseLong(item.path("play_count").asText("0")); } catch (Exception ignored) {}

                                    if (newScore > prevScore || (newScore == prevScore && newPlays > prevPlays)) {
                                        bestByNorm.put(norm, item);
                                    }
                                }
                            }

                            for (JsonNode item : bestByNorm.values()) {
                                if (resultList.size() >= targetCount) break;

                                try {
                                    String rawId = item.path("id").asText();
                                    if (rawId == null || rawId.isBlank()) continue;

                                    String title = cleanHtml(item.path("title").asText("Unknown Title"));
                                    String normTitle = title.toLowerCase().replaceAll("[^a-z0-9]", "");
                                    if (seenNormalizedTitles.contains(normTitle)) {
                                        continue;
                                    }
                                    seenNormalizedTitles.add(normTitle);

                                    String sourceId = "saavn_" + rawId;
                                    String subtitle = cleanHtml(item.path("subtitle").asText("Various Artists"));
                                    String language = item.path("language").asText("Hindi");
                                    
                                    JsonNode moreInfo = item.path("more_info");
                                    String albumName = cleanHtml(moreInfo.path("album").asText("Single"));
                                    String vlink = moreInfo.path("vlink").asText("");
                                    String mediaPreview = moreInfo.path("media_preview_url").asText("");
                                    String encryptedMediaUrl = moreInfo.path("encrypted_media_url").asText("");

                                    // Decrypt full-length 320kbps master studio audio URL
                                    String decryptedUrl = decryptMediaUrl(encryptedMediaUrl);
                                    String audioUrl;
                                    if (decryptedUrl != null && !decryptedUrl.isBlank()) {
                                        audioUrl = decryptedUrl;
                                    } else if (!vlink.isBlank()) {
                                        audioUrl = vlink;
                                    } else if (!mediaPreview.isBlank()) {
                                        audioUrl = mediaPreview;
                                    } else {
                                        audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3";
                                    }

                                    // High-definition 500x500 authentic movie poster / album artwork
                                    String rawImage = item.path("image").asText("");
                                    String imageUrl = rawImage
                                        .replace("150x150", "500x500")
                                        .replace("50x50", "500x500");
                                    if (imageUrl.isBlank()) {
                                        imageUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=500";
                                    }

                                    int durationSec = 210;
                                    try {
                                        String durStr = moreInfo.path("duration").asText();
                                        if (!durStr.isBlank()) {
                                            durationSec = Integer.parseInt(durStr);
                                        }
                                    } catch (Exception ignored) {}

                                    long playCount = 50000L;
                                    try {
                                        String pcStr = item.path("play_count").asText();
                                        if (!pcStr.isBlank()) {
                                            playCount = Long.parseLong(pcStr);
                                        }
                                    } catch (Exception ignored) {}

                                    // Extract primary artist name
                                    String primaryArtistName = subtitle;
                                    if (primaryArtistName.contains("-")) {
                                        primaryArtistName = primaryArtistName.split("-")[0].trim();
                                    }
                                    if (primaryArtistName.contains(",")) {
                                        primaryArtistName = primaryArtistName.split(",")[0].trim();
                                    }
                                    if (primaryArtistName.isBlank()) {
                                        primaryArtistName = "Bollywood Artists";
                                    }

                                    // Check if song already exists in database (by sourceId or title)
                                    Optional<Song> existingOpt = songRepository.findBySourceId(sourceId);
                                    if (existingOpt.isEmpty()) {
                                        List<Song> existingByTitle = songRepository.findByTitleIgnoreCase(title);
                                        if (!existingByTitle.isEmpty()) {
                                            existingOpt = Optional.of(existingByTitle.get(0));
                                        }
                                    }
                                    Song song;

                                    if (existingOpt.isPresent()) {
                                        song = existingOpt.get();
                                        boolean needsUpdate = false;
                                        if (audioUrl.contains("_320.mp4") && (song.getAudioUrl() == null || !song.getAudioUrl().contains("_320.mp4"))) {
                                            song.setAudioUrl(audioUrl);
                                            needsUpdate = true;
                                        }
                                        if (imageUrl != null && !imageUrl.isBlank()) {
                                            if (song.getImageUrl() == null || song.getImageUrl().contains("Sunset-Strings") || 
                                                song.getImageUrl().contains("150x150") || song.getImageUrl().contains("50x50") ||
                                                song.getImageUrl().contains("unsplash")) {
                                                song.setImageUrl(imageUrl);
                                                needsUpdate = true;
                                            }
                                        }
                                        if (durationSec > 0 && (song.getDurationSec() == null || song.getDurationSec() < durationSec)) {
                                            song.setDurationSec(durationSec);
                                            needsUpdate = true;
                                        }
                                        if (needsUpdate) {
                                            song = songRepository.save(song);
                                        }
                                    } else {
                                        // Persist Artist
                                        final String artistNameToFind = primaryArtistName;
                                        final String finalImageUrl = imageUrl;
                                        Artist artist = artistRepository.findByNameIgnoreCase(artistNameToFind)
                                            .orElseGet(() -> artistRepository.save(
                                                Artist.builder()
                                                    .name(artistNameToFind)
                                                    .imageUrl(finalImageUrl)
                                                    .build()
                                            ));

                                        // Persist Album
                                        final String albumNameToFind = albumName;
                                        Album album = albumRepository.findByNameContainingIgnoreCase(albumNameToFind, org.springframework.data.domain.PageRequest.of(0, 1))
                                            .getContent().stream().findFirst()
                                            .orElseGet(() -> albumRepository.save(
                                                Album.builder()
                                                    .name(albumNameToFind)
                                                    .artist(artist)
                                                    .imageUrl(finalImageUrl)
                                                    .build()
                                            ));

                                        // Persist Song to PostgreSQL
                                        song = Song.builder()
                                            .sourceId(sourceId)
                                            .title(title)
                                            .artist(artist)
                                            .album(album)
                                            .genre(capitalize(language))
                                            .durationSec(durationSec)
                                            .audioUrl(audioUrl)
                                            .imageUrl(imageUrl)
                                            .playCount(playCount)
                                            .active(true)
                                            .license("JioSaavn 320kbps Stream")
                                            .build();

                                        song = songRepository.save(song);
                                    }

                                    boolean isLiked = false;
                                    if (currentUserId != null) {
                                        isLiked = likedSongRepository.existsByUserIdAndSongId(currentUserId, song.getId());
                                    }

                                    resultList.add(SongDto.builder()
                                        .id(song.getId())
                                        .sourceId(song.getSourceId())
                                        .title(song.getTitle())
                                        .artistId(song.getArtist().getId())
                                        .artistName(song.getArtist().getName())
                                        .albumId(song.getAlbum() != null ? song.getAlbum().getId() : null)
                                        .albumName(song.getAlbum() != null ? song.getAlbum().getName() : "Single")
                                        .genre(song.getGenre())
                                        .durationSec(song.getDurationSec())
                                        .audioUrl(song.getAudioUrl())
                                        .imageUrl(song.getImageUrl())
                                        .playCount(song.getPlayCount())
                                        .active(song.isActive())
                                        .isLiked(isLiked)
                                        .build());

                                } catch (Exception itemEx) {
                                    log.debug("Error processing search item: {}", itemEx.getMessage());
                                }
                            }
                        }
                    }
                } catch (Exception pageEx) {
                    log.debug("Error on page {}: {}", page, pageEx.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("JioSaavn search query '{}' failed: {}", query, e.getMessage());
        }

        return resultList;
    }

    /**
     * Autocomplete suggestions using JioSaavn live search autocomplete API.
     */
    public com.musicapp.dto.CatalogDto.AutocompleteResultDto getAutocomplete(String query) {
        if (query == null || query.trim().length() < 2) {
            return com.musicapp.dto.CatalogDto.AutocompleteResultDto.builder()
                .query(query)
                .suggestions(java.util.Collections.emptyList())
                .songs(java.util.Collections.emptyList())
                .artists(java.util.Collections.emptyList())
                .build();
        }

        List<String> suggestions = new ArrayList<>();
        List<SongDto> songs = new ArrayList<>();
        List<com.musicapp.dto.CatalogDto.ArtistDto> artists = new ArrayList<>();

        try {
            String encoded = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);
            String url = String.format(
                "https://www.jiosaavn.com/api.php?__call=autocomplete.get&_format=json&_marker=0&api_version=4&ctx=web6dot0&query=%s",
                encoded
            );

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .timeout(Duration.ofSeconds(4))
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 && response.body() != null) {
                JsonNode root = objectMapper.readTree(response.body());

                // Top query
                JsonNode topQuery = root.path("topquery").path("data");
                if (topQuery.isArray() && !topQuery.isEmpty()) {
                    String topTitle = cleanHtml(topQuery.get(0).path("title").asText(""));
                    if (!topTitle.isBlank() && !suggestions.contains(topTitle)) {
                        suggestions.add(topTitle);
                    }
                }

                // Songs
                JsonNode songsNode = root.path("songs").path("data");
                if (songsNode.isArray()) {
                    for (JsonNode s : songsNode) {
                        String stitle = cleanHtml(s.path("title").asText(""));
                        if (!stitle.isBlank() && !suggestions.contains(stitle)) {
                            suggestions.add(stitle);
                        }
                    }
                }

                // Artists
                JsonNode artistsNode = root.path("artists").path("data");
                if (artistsNode.isArray()) {
                    for (JsonNode a : artistsNode) {
                        String atitle = cleanHtml(a.path("title").asText(""));
                        if (!atitle.isBlank() && !suggestions.contains(atitle)) {
                            suggestions.add(atitle);
                        }
                        String aImg = a.path("image").asText("");
                        artists.add(com.musicapp.dto.CatalogDto.ArtistDto.builder()
                            .id(null)
                            .name(atitle)
                            .imageUrl(aImg)
                            .build());
                    }
                }

                // Albums
                JsonNode albumsNode = root.path("albums").path("data");
                if (albumsNode.isArray()) {
                    for (JsonNode al : albumsNode) {
                        String altitle = cleanHtml(al.path("title").asText(""));
                        if (!altitle.isBlank() && !suggestions.contains(altitle)) {
                            suggestions.add(altitle);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Autocomplete fetch failed: {}", e.getMessage());
        }

        return com.musicapp.dto.CatalogDto.AutocompleteResultDto.builder()
            .query(query)
            .suggestions(suggestions)
            .songs(songs)
            .artists(artists)
            .build();
    }

    private String cleanHtml(String text) {
        if (text == null) return "";
        return text.replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&#039;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">");
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return "Hindi";
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    private static int calculateOriginalScore(JsonNode item) {
        if (item == null) return 0;
        int score = 0;
        String album = item.path("more_info").path("album").asText("").toLowerCase();
        String subtitle = item.path("subtitle").asText("").toLowerCase();
        String text = album + " " + subtitle;

        if (text.contains("vol") || text.contains("volume")) score -= 50;
        if (text.contains("lounge")) score -= 50;
        if (text.contains("special")) score -= 30;
        if (text.contains("hits")) score -= 40;
        if (text.contains("mix") || text.contains("remix")) score -= 30;
        if (text.contains("spotlight")) score -= 40;
        if (text.contains("chill")) score -= 30;
        if (text.contains("best of")) score -= 50;
        if (text.contains("sunset & strings") || text.contains("sunset and strings")) score -= 70;
        if (text.contains("collection") || text.contains("compilation")) score -= 50;
        if (text.contains("party") || text.contains("dance mix")) score -= 30;
        if (text.contains("mashup")) score -= 40;

        if (!album.isBlank() && !album.equalsIgnoreCase("single")) {
            score += 25;
        }
        return score;
    }
}
