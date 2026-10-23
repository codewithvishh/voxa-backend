package com.musicapp.controller;

import com.musicapp.dto.AdminDto.*;
import com.musicapp.dto.AuthDto.UserProfileDto;
import com.musicapp.model.ImportJob;
import com.musicapp.service.AdminService;
import com.musicapp.service.JamendoImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin Operations", description = "Endpoints for administrators to manage users, catalog, and ingestion")
public class AdminController {

    private final AdminService adminService;
    private final JamendoImportService jamendoImportService;

    public AdminController(AdminService adminService, JamendoImportService jamendoImportService) {
        this.adminService = adminService;
        this.jamendoImportService = jamendoImportService;
    }

    @GetMapping("/stats")
    @Operation(summary = "Get platform metrics (songs, users, total plays, last import status)")
    public ResponseEntity<AdminStatsDto> getStats() {
        return ResponseEntity.ok(adminService.getStats());
    }

    @GetMapping("/users")
    @Operation(summary = "View paginated list of all registered users")
    public ResponseEntity<Page<UserProfileDto>> getUsers(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(adminService.getAllUsers(page, size));
    }

    @PatchMapping("/users/{id}/status")
    @Operation(summary = "Enable or disable a user account")
    public ResponseEntity<Map<String, String>> toggleUserStatus(
        @PathVariable Long id,
        @RequestBody UpdateSongStatusRequest request
    ) {
        adminService.toggleUserStatus(id, request.isActive());
        return ResponseEntity.ok(Collections.singletonMap("message", "User status updated"));
    }

    @PatchMapping("/songs/{id}/status")
    @Operation(summary = "Enable, hide or disable a song in the catalog")
    public ResponseEntity<Map<String, String>> toggleSongStatus(
        @PathVariable Long id,
        @RequestBody UpdateSongStatusRequest request
    ) {
        adminService.setSongActiveStatus(id, request.isActive());
        return ResponseEntity.ok(Collections.singletonMap("message", "Song status updated"));
    }

    @PostMapping("/import")
    @Operation(summary = "Trigger asynchronous Jamendo music catalog import")
    public ResponseEntity<ImportStatusDto> triggerImport(@RequestBody(required = false) ImportTriggerRequest request) {
        int max = request != null ? request.getMax() : 20;
        String tag = request != null ? request.getTag() : null;

        ImportJob job = jamendoImportService.createJob(max);
        jamendoImportService.runImportAsync(job.getId(), max, tag);

        return ResponseEntity.ok(jamendoImportService.mapToJobDto(job));
    }

    @GetMapping("/import/{jobId}")
    @Operation(summary = "Get progress status of a specific import job")
    public ResponseEntity<ImportStatusDto> getImportStatus(@PathVariable Long jobId) {
        return ResponseEntity.ok(jamendoImportService.getJobStatus(jobId));
    }

    @GetMapping("/import")
    @Operation(summary = "View history and execution logs of all import jobs")
    public ResponseEntity<List<ImportStatusDto>> getImportHistory() {
        return ResponseEntity.ok(jamendoImportService.getAllJobs());
    }
}
