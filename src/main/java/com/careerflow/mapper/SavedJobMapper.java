package com.careerflow.mapper;

import com.careerflow.dto.response.SavedJobResponse;
import com.careerflow.entity.SavedJob;
import org.springframework.stereotype.Component;

@Component
public class SavedJobMapper {

    public SavedJobResponse toResponse(SavedJob savedJob) {

        return SavedJobResponse.builder()
                .id(savedJob.getId())
                .jobId(savedJob.getJob().getId())
                .jobSeekerId(savedJob.getJobSeeker().getId())
                .jobTitle(savedJob.getJob().getTitle())
                .companyName(savedJob.getJob().getEmployer().getCompany().getName())
                .location(savedJob.getJob().getLocation())
                .employmentType(savedJob.getJob().getEmploymentType())
                .experienceLevel(savedJob.getJob().getExperienceLevel())
                .savedAt(savedJob.getSavedAt())
                .build();
    }
}