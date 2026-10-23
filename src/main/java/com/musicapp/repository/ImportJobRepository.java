package com.musicapp.repository;

import com.musicapp.model.ImportJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImportJobRepository extends JpaRepository<ImportJob, Long> {
    List<ImportJob> findAllByOrderByStartedAtDesc();
}
