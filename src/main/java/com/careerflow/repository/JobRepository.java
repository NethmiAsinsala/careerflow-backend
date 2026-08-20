package com.careerflow.repository;

import com.careerflow.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {

    List<Job> findByEmployerId(Long employerId);

    List<Job> findByStatus(String status);

    List<Job> findByEmployerIdAndStatus(Long employerId, String status);
}