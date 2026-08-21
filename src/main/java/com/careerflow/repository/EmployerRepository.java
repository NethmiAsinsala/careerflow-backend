package com.careerflow.repository;

import com.careerflow.entity.Employer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmployerRepository extends JpaRepository<Employer, Long> {

    Optional<Employer> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}