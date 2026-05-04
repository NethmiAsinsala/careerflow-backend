package com.careerflow.controller;

import com.careerflow.dto.request.SavedJobRequest;
import com.careerflow.dto.response.SavedJobResponse;
import com.careerflow.service.SavedJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/saved-jobs")
@RequiredArgsConstructor
public class SavedJobController {

    private final SavedJobService savedJobService;

    @PostMapping
    public ResponseEntity<SavedJobResponse> saveJob(
            @RequestBody SavedJobRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(savedJobService.saveJob(request));
    }

    @GetMapping("/job-seeker/{jobSeekerId}")
    public ResponseEntity<List<SavedJobResponse>> getSavedJobs(
            @PathVariable Long jobSeekerId) {

        return ResponseEntity.ok(
                savedJobService.getSavedJobs(jobSeekerId)
        );
    }

    @DeleteMapping("/job/{jobId}/job-seeker/{jobSeekerId}")
    public ResponseEntity<Void> removeSavedJob(
            @PathVariable Long jobId,
            @PathVariable Long jobSeekerId) {

        savedJobService.removeSavedJob(jobId, jobSeekerId);

        return ResponseEntity.noContent().build();
    }
}