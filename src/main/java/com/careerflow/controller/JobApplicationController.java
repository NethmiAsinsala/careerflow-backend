package com.careerflow.controller;

import com.careerflow.dto.request.JobApplicationRequest;
import com.careerflow.dto.response.JobApplicationResponse;
import com.careerflow.service.JobApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/job-applications")
@RequiredArgsConstructor
public class JobApplicationController {

    private final JobApplicationService jobApplicationService;

    @PostMapping("/job/{jobId}/job-seeker/{jobSeekerId}")
    public ResponseEntity<JobApplicationResponse> applyForJob(
            @PathVariable Long jobId,
            @PathVariable Long jobSeekerId,
            @RequestBody JobApplicationRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(jobApplicationService.applyForJob(
                        jobId,
                        jobSeekerId,
                        request
                ));
    }

    @GetMapping
    public ResponseEntity<List<JobApplicationResponse>> getAllApplications() {
        return ResponseEntity.ok(
                jobApplicationService.getAllApplications()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobApplicationResponse> getApplicationById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                jobApplicationService.getApplicationById(id)
        );
    }

    @GetMapping("/job/{jobId}")
    public ResponseEntity<List<JobApplicationResponse>> getApplicationsByJob(
            @PathVariable Long jobId
    ) {
        return ResponseEntity.ok(
                jobApplicationService.getApplicationsByJob(jobId)
        );
    }

    @GetMapping("/job-seeker/{jobSeekerId}")
    public ResponseEntity<List<JobApplicationResponse>> getApplicationsByJobSeeker(
            @PathVariable Long jobSeekerId
    ) {
        return ResponseEntity.ok(
                jobApplicationService.getApplicationsByJobSeeker(jobSeekerId)
        );
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<JobApplicationResponse>> getApplicationsByStatus(
            @PathVariable String status
    ) {
        return ResponseEntity.ok(
                jobApplicationService.getApplicationsByStatus(status)
        );
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<JobApplicationResponse> updateApplicationStatus(
            @PathVariable Long id,
            @RequestParam String status
    ) {
        return ResponseEntity.ok(
                jobApplicationService.updateApplicationStatus(
                        id,
                        status
                )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> withdrawApplication(
            @PathVariable Long id
    ) {
        jobApplicationService.withdrawApplication(id);

        return ResponseEntity.noContent().build();
    }
}