package com.careerflow.service;

import com.careerflow.dto.request.JobApplicationRequest;
import com.careerflow.dto.response.JobApplicationResponse;

import java.util.List;

public interface JobApplicationService {

    JobApplicationResponse applyForJob(
            Long jobId,
            Long jobSeekerId,
            JobApplicationRequest request
    );

    List<JobApplicationResponse> getAllApplications();

    JobApplicationResponse getApplicationById(Long id);

    List<JobApplicationResponse> getApplicationsByJob(Long jobId);

    List<JobApplicationResponse> getApplicationsByJobSeeker(Long jobSeekerId);

    List<JobApplicationResponse> getApplicationsByStatus(String status);

    JobApplicationResponse updateApplicationStatus(
            Long id,
            String status
    );

    void withdrawApplication(Long id);
}