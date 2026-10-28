package com.musicapp.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class RootHealthController {

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
}
