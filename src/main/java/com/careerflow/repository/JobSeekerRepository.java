package com.careerflow.repository;

import com.careerflow.entity.JobSeeker;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobSeekerRepository extends JpaRepository<JobSeeker, Long> {
    java.util.Optional<JobSeeker> findByUserId(Long userId);
    boolean existsByIdAndUser_Id(Long id, Long userId);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select s from JobSeeker s where s.id = :id")
    java.util.Optional<JobSeeker> findByIdForResumeUpdate(@org.springframework.data.repository.query.Param("id") Long id);
}