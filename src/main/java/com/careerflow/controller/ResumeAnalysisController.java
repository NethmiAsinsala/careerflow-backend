package com.careerflow.controller;
import com.careerflow.service.ResumeAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.CacheControl;
import io.swagger.v3.oas.annotations.Operation;
@RestController @RequestMapping("/api/ai/resume") @RequiredArgsConstructor
public class ResumeAnalysisController {
    private final ResumeAnalysisService analysis;
    @PostMapping("/analyze")
    @Operation(summary="Analyze your uploaded resume with local Ollama", description="No request body. Returns suggestions without updating your profile or storing analysis history.")
    public ResponseEntity<ResumeAnalysisService.Analysis> analyze() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(analysis.analyze());
    }
    @PostMapping("/compare/{jobId}")
    @Operation(summary="Compare your uploaded CV with a job", description="Candidate only. Returns CV skill coverage and job-specific advice; no profile changes or saved analysis.")
    public ResponseEntity<ResumeAnalysisService.JobAnalysis> compare(@PathVariable Long jobId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(analysis.compare(jobId));
    }
}
