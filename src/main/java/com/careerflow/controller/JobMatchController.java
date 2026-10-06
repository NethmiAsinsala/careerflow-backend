package com.careerflow.controller;

import com.careerflow.dto.response.JobMatchResponse;
import com.careerflow.service.JobMatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobMatchController {
    private final JobMatchService service;
    @GetMapping("/{jobId}/match")
    public JobMatchResponse match(@PathVariable Long jobId) { return service.match(jobId); }
}
