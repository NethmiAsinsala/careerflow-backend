package com.careerflow.service;

import com.careerflow.dto.request.JobRequest;
import com.careerflow.entity.ApplicationStatus;
import com.careerflow.entity.Job;
import com.careerflow.entity.JobStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class JobWorkflowRules {
    private final Clock clock;

    public String validateJob(JobRequest request, String currentStatus) {
        String requested = request.getStatus();
        String effective = requested != null ? requested : (currentStatus != null ? currentStatus : "OPEN");
        JobStatus status = requestedJobStatus(effective);
        if (request.getSalaryMin() != null && request.getSalaryMin().signum() < 0
                || request.getSalaryMax() != null && request.getSalaryMax().signum() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Salary must not be negative");
        }
        if (request.getSalaryMin() != null && request.getSalaryMax() != null
                && request.getSalaryMin().compareTo(request.getSalaryMax()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Minimum salary must not exceed maximum salary");
        }
        if (status == JobStatus.OPEN && request.getApplicationDeadline() != null
                && !request.getApplicationDeadline().isAfter(LocalDateTime.now(clock))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "An OPEN job must have a future application deadline");
        }
        return status.name();
    }

    public void requireAcceptingApplications(Job job) {
        if (!JobStatus.OPEN.name().equals(job.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This job is not open for applications");
        }
        if (job.getApplicationDeadline() != null
                && !job.getApplicationDeadline().isAfter(LocalDateTime.now(clock))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The application deadline has passed");
        }
    }

    public JobStatus requestedJobStatus(String value) {
        try {
            return JobStatus.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Job status must be OPEN or CLOSED");
        }
    }

    public ApplicationStatus requestedApplicationStatus(String value) {
        try {
            return ApplicationStatus.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Application status must be APPLIED, SHORTLISTED, INTERVIEW, OFFERED, HIRED or REJECTED");
        }
    }

    public String nextApplicationStatus(String currentValue, String requestedValue) {
        ApplicationStatus next = requestedApplicationStatus(requestedValue);
        ApplicationStatus current;
        try {
            current = ApplicationStatus.valueOf(currentValue);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The stored application status needs correction before review");
        }
        if (!current.canTransitionTo(next)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot change application status from " + current + " to " + next);
        }
        return next.name();
    }
}
