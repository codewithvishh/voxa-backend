package com.musicapp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class RootHealthController {

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> rootPing() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "Voxa Music Backend API",
            "message", "Service is healthy and active"
        ));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthPing() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "Voxa Music Backend API"
        ));
    }

    @GetMapping({"/health/redis", "/api/health/redis"})
    public ResponseEntity<Map<String, Object>> redisHealth() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "Voxa Redis Cache Diagnostic");

        if (redisTemplate == null) {
            response.put("status", "DISABLED");
            response.put("message", "RedisTemplate bean not configured");
            return ResponseEntity.ok(response);
        }

        long start = System.currentTimeMillis();
        try {
            redisTemplate.opsForValue().get("health_ping");
            long latencyMs = System.currentTimeMillis() - start;

            response.put("status", "CONNECTED");
            response.put("latencyMs", latencyMs + "ms");
            response.put("message", "Redis cache is ACTIVE and accelerating queries!");
        } catch (Exception e) {
            response.put("status", "DISCONNECTED");
            response.put("error", e.getMessage());
            response.put("message", "Redis server is not reachable at configured host/port.");
        }

        return ResponseEntity.ok(response);
    }
}
