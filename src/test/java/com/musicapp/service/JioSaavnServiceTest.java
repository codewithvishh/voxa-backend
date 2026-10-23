package com.musicapp.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class JioSaavnServiceTest {

    @Test
    public void testDecryptMediaUrlReturns320kbps() {
        // Sample encrypted media URL from JioSaavn for "Tum Ho Toh"
        String encrypted = "ID2ieOjCrwfgWvL5sXl4B1ImC5QfbsDy1UxuKKz+UKI3G7CEBdSAaQrPVZF+LqWeT7xPg3C7M2Uy2ty7TTvFJxw7tS9a8Gtq";
        String decrypted = JioSaavnService.decryptMediaUrl(encrypted);
        
        assertNotNull(decrypted);
        assertTrue(decrypted.startsWith("https://"));
        assertTrue(decrypted.contains("_320.mp4"), "Expected URL to be upgraded to 320kbps stream, got: " + decrypted);
        System.out.println("DECRYPTED HIGH QUALITY URL: " + decrypted);
    }
}
