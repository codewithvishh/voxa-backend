package com.musicapp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "import_jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ImportStatus status = ImportStatus.PENDING;

    @Builder.Default
    @Column(nullable = false)
    private int requested = 0;

    @Builder.Default
    @Column(nullable = false)
    private int imported = 0;

    @Builder.Default
    @Column(nullable = false)
    private int failed = 0;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    @Column(columnDefinition = "TEXT")
    private String errorLog;
}
