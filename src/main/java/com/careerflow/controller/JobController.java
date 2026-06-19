package com.careerflow.controller;

import com.careerflow.dto.request.JobRequest;
import com.careerflow.dto.response.JobResponse;
import com.careerflow.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @PostMapping("/employer/{employerId}")
    public ResponseEntity<JobResponse> createJob(
            @PathVariable Long employerId,
            @Valid @RequestBody JobRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(jobService.createJob(employerId, request));
    }

    @GetMapping
    public ResponseEntity<List<JobResponse>> getAllJobs() {

        return ResponseEntity.ok(
                jobService.getAllJobs()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> getJobById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                jobService.getJobById(id)
        );
    }

    @GetMapping("/employer/{employerId}")
    public ResponseEntity<List<JobResponse>> getJobsByEmployerId(
            @PathVariable Long employerId) {

        return ResponseEntity.ok(
                jobService.getJobsByEmployerId(employerId)
        );
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<JobResponse>> getJobsByStatus(
            @PathVariable String status) {

        return ResponseEntity.ok(
                jobService.getJobsByStatus(status)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<JobResponse> updateJob(
            @PathVariable Long id,
            @Valid @RequestBody JobRequest request) {

        return ResponseEntity.ok(
                jobService.updateJob(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJob(
            @PathVariable Long id) {

        jobService.deleteJob(id);

        return ResponseEntity.noContent().build();
    }
}