package com.careerflow.repository;

import com.careerflow.entity.Employer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployerRepository extends JpaRepository<Employer, Long> {
}