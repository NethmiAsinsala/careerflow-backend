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
import com.careerflow.service.JobWorkflowRules;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
@PreAuthorize("denyAll()")
@RequiredArgsConstructor
@Transactional
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final EmployerRepository employerRepository;
    private final JobMapper jobMapper;
    private final JobWorkflowRules workflow;

    @Override
    @PreAuthorize("@resourceAccess.managesEmployer(#employerId)")
    public JobResponse createJob(Long employerId, JobRequest request) {

        Employer employer = employerRepository.findById(employerId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "Employer not found with id: " + employerId
                        ));

        String status = workflow.validateJob(request, null);
        Job job = jobMapper.toEntity(request);
        job.setStatus(status);
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
                        new ResponseStatusException(HttpStatus.NOT_FOUND,
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

        workflow.requestedJobStatus(status);
        return jobRepository.findByStatus(status)
                .stream()
                .map(jobMapper::toResponse)
                .toList();
    }

    @Override
    @PreAuthorize("@resourceAccess.managesJob(#id)")
    public JobResponse updateJob(Long id, JobRequest request) {

        Job job = jobRepository.findByIdForUpdate(id)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "Job not found with id: " + id
                        ));

        String status = workflow.validateJob(request, job.getStatus());
        jobMapper.updateEntity(job, request);
        job.setStatus(status);

        return jobMapper.toResponse(
                jobRepository.save(job)
        );
    }

    @Override
    @PreAuthorize("@resourceAccess.managesJob(#id)")
    public void deleteJob(Long id) {

        Job job = jobRepository.findByIdForUpdate(id)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "Job not found with id: " + id
                        ));

        jobRepository.delete(job);
    }
}
