package com.careerflow.repository;

import com.careerflow.entity.Education;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EducationRepository extends JpaRepository<Education, Long> {
    java.util.List<Education> findByJobSeekerIdOrderByIdAsc(Long jobSeekerId);
}