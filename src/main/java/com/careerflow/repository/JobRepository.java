package com.careerflow.repository;

import com.careerflow.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Job> {

    List<Job> findByEmployerId(Long employerId);

    List<Job> findByStatus(String status);

    List<Job> findByEmployerIdAndStatus(Long employerId, String status);
    boolean existsByIdAndEmployer_User_Id(Long id, Long userId);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select record from Job record where record.id = :id")
    java.util.Optional<Job> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);
}