package com.careerflow.repository;

import com.careerflow.entity.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JobApplicationRepository
        extends JpaRepository<JobApplication, Long> {

    List<JobApplication> findByJobId(Long jobId);

    List<JobApplication> findByJobSeekerId(Long jobSeekerId);

    List<JobApplication> findByStatus(String status);

    Optional<JobApplication> findByJobIdAndJobSeekerId(
            Long jobId,
            Long jobSeekerId
    );

    boolean existsByJobIdAndJobSeekerId(
            Long jobId,
            Long jobSeekerId
    );
}