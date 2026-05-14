package com.careerflow.service.impl;

import com.careerflow.dto.request.JobApplicationRequest;
import com.careerflow.dto.response.JobApplicationResponse;
import com.careerflow.entity.Job;
import com.careerflow.entity.JobApplication;
import com.careerflow.entity.JobSeeker;
import com.careerflow.mapper.JobApplicationMapper;
import com.careerflow.repository.JobApplicationRepository;
import com.careerflow.repository.JobRepository;
import com.careerflow.repository.JobSeekerRepository;
import com.careerflow.service.JobApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class JobApplicationServiceImpl implements JobApplicationService {

    private final JobApplicationRepository jobApplicationRepository;
    private final JobRepository jobRepository;
    private final JobSeekerRepository jobSeekerRepository;
    private final JobApplicationMapper mapper;

    @Override
    public JobApplicationResponse applyForJob(
            Long jobId,
            Long jobSeekerId,
            JobApplicationRequest request
    ) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() ->
                        new RuntimeException("Job not found with id: " + jobId));

        JobSeeker jobSeeker = jobSeekerRepository.findById(jobSeekerId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Job seeker not found with id: " + jobSeekerId
                        ));

        if (jobApplicationRepository.existsByJobIdAndJobSeekerId(
                jobId, jobSeekerId)) {
            throw new RuntimeException(
                    "Job seeker has already applied for this job"
            );
        }

        JobApplication application = JobApplication.builder()
                .job(job)
                .jobSeeker(jobSeeker)
                .coverLetter(request.getCoverLetter())
                .resumeUrl(request.getResumeUrl())
                .status("APPLIED")
                .build();

        return mapper.toResponse(
                jobApplicationRepository.save(application)
        );
    }

    @Override
    public List<JobApplicationResponse> getAllApplications() {
        return jobApplicationRepository.findAll()
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    public JobApplicationResponse getApplicationById(Long id) {
        JobApplication application = getApplication(id);

        return mapper.toResponse(application);
    }

    @Override
    public List<JobApplicationResponse> getApplicationsByJob(Long jobId) {
        return jobApplicationRepository.findByJobId(jobId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    public List<JobApplicationResponse> getApplicationsByJobSeeker(
            Long jobSeekerId
    ) {
        return jobApplicationRepository.findByJobSeekerId(jobSeekerId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    public List<JobApplicationResponse> getApplicationsByStatus(
            String status
    ) {
        return jobApplicationRepository.findByStatus(status)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    public JobApplicationResponse updateApplicationStatus(
            Long id,
            String status
    ) {
        JobApplication application = getApplication(id);

        application.setStatus(status);

        return mapper.toResponse(
                jobApplicationRepository.save(application)
        );
    }

    @Override
    public void withdrawApplication(Long id) {
        JobApplication application = getApplication(id);

        jobApplicationRepository.delete(application);
    }

    private JobApplication getApplication(Long id) {
        return jobApplicationRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Job application not found with id: " + id
                        ));
    }
}