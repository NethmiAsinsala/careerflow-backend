package com.careerflow.repository;

import com.careerflow.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillRepository extends JpaRepository<Skill, Long> {
    java.util.List<Skill> findByJobSeekerIdOrderByIdAsc(Long jobSeekerId);
}