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
import java.util.Set;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.careerflow.dto.request.JobSearchRequest;
import com.careerflow.dto.response.PageResponse;
import com.careerflow.specification.JobSpecifications;
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
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public PageResponse<JobResponse> searchJobs(JobSearchRequest request) {
        if (request.getSalaryMin() != null && request.getSalaryMax() != null
                && request.getSalaryMin().compareTo(request.getSalaryMax()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Minimum salary must not exceed maximum salary");
        }
        String[] sortParts = request.getSort().split(",", -1);
        Set<String> sortable = Set.of("createdAt", "title", "salaryMin", "salaryMax", "applicationDeadline", "id");
        if (sortParts.length != 2 || !sortable.contains(sortParts[0])
                || !(sortParts[1].equalsIgnoreCase("asc") || sortParts[1].equalsIgnoreCase("desc"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Sort must be field,asc or field,desc; fields: createdAt, title, salaryMin, salaryMax, applicationDeadline, id");
        }
        Sort.Direction direction = Sort.Direction.fromString(sortParts[1]);
        Sort sort = Sort.by(direction, sortParts[0]);
        if (!sortParts[0].equals("id")) sort = sort.and(Sort.by(direction, "id"));
        var page = PageRequest.of(request.getPage(), request.getSize(), sort);
        return PageResponse.from(jobRepository.findAll(
                JobSpecifications.matching(request, LocalDateTime.now(clock)), page).map(jobMapper::toResponse));
    }


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
