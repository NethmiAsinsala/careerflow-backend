package com.careerflow.repository;

import com.careerflow.entity.SavedJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SavedJobRepository extends JpaRepository<SavedJob, Long> {

    List<SavedJob> findByJobSeekerId(Long jobSeekerId);

    List<SavedJob> findByJobId(Long jobId);

    Optional<SavedJob> findByJobIdAndJobSeekerId(
            Long jobId,
            Long jobSeekerId
    );

    boolean existsByJobIdAndJobSeekerId(
            Long jobId,
            Long jobSeekerId
    );
}