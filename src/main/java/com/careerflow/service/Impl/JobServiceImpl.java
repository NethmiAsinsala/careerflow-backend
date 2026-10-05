package com.careerflow.service.impl;

import com.careerflow.dto.request.JobRequest;
import com.careerflow.dto.response.JobResponse;
import com.careerflow.entity.Employer;
import com.careerflow.entity.Job;
import com.careerflow.mapper.JobMapper;
import com.careerflow.repository.EmployerRepository;
import com.careerflow.repository.JobRepository;
import com.careerflow.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@PreAuthorize("denyAll()")
@RequiredArgsConstructor
@Transactional
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final EmployerRepository employerRepository;
    private final JobMapper jobMapper;

    @Override
    @PreAuthorize("@resourceAccess.managesEmployer(#employerId)")
    public JobResponse createJob(Long employerId, JobRequest request) {

        Employer employer = employerRepository.findById(employerId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Employer not found with id: " + employerId
                        ));

        Job job = jobMapper.toEntity(request);
        job.setEmployer(employer);

        return jobMapper.toResponse(
                jobRepository.save(job)
        );
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public List<JobResponse> getAllJobs() {

        return jobRepository.findAll()
                .stream()
                .map(jobMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public JobResponse getJobById(Long id) {

        Job job = jobRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Job not found with id: " + id
                        ));

        return jobMapper.toResponse(job);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public List<JobResponse> getJobsByEmployerId(Long employerId) {

        return jobRepository.findByEmployerId(employerId)
                .stream()
                .map(jobMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public List<JobResponse> getJobsByStatus(String status) {

        return jobRepository.findByStatus(status)
                .stream()
                .map(jobMapper::toResponse)
                .toList();
    }

    @Override
    @PreAuthorize("@resourceAccess.managesJob(#id)")
    public JobResponse updateJob(Long id, JobRequest request) {

        Job job = jobRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Job not found with id: " + id
                        ));

        jobMapper.updateEntity(job, request);

        return jobMapper.toResponse(
                jobRepository.save(job)
        );
    }

    @Override
    @PreAuthorize("@resourceAccess.managesJob(#id)")
    public void deleteJob(Long id) {

        Job job = jobRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Job not found with id: " + id
                        ));

        jobRepository.delete(job);
    }
}