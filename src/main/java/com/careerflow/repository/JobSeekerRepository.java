package com.careerflow.repository;

import com.careerflow.entity.JobSeeker;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobSeekerRepository extends JpaRepository<JobSeeker, Long> {
}