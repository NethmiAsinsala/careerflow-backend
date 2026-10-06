package com.careerflow.repository;
import com.careerflow.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ResumeRepository extends JpaRepository<Resume,Long> { }
