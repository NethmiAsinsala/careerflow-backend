package com.careerflow.service;

import com.careerflow.dto.request.JobRequest;
import com.careerflow.dto.response.JobResponse;

import java.util.List;

public interface JobService {

    com.careerflow.dto.response.PageResponse<JobResponse> searchJobs(com.careerflow.dto.request.JobSearchRequest request);

    JobResponse createJob(Long employerId, JobRequest request);

    List<JobResponse> getAllJobs();

    JobResponse getJobById(Long id);

    List<JobResponse> getJobsByEmployerId(Long employerId);

    List<JobResponse> getJobsByStatus(String status);

    JobResponse updateJob(Long id, JobRequest request);

    void deleteJob(Long id);
}