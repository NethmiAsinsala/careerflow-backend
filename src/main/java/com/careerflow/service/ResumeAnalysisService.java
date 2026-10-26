package com.careerflow.service;

import com.careerflow.ai.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.Semaphore;

@Service @RequiredArgsConstructor
@PreAuthorize("hasRole('JOB_SEEKER')")
public class ResumeAnalysisService {
    private final ResumeService resumes;
    private final ResumeTextExtractor extractor;
    private final OllamaResumeClient ai;
    private final com.careerflow.repository.JobRepository jobs;
    private final SkillMatcher matcher;
    private final Semaphore capacity = new Semaphore(1);
    public record Analysis(String provider, String model, String resumeSha256, int pages,
                           ResumeFeedback feedback, String note) { }
    public record Comparison(Long jobId, String jobTitle, Integer percentage, java.util.List<String> matchedSkills,
                             java.util.List<String> missingSkills, int requiredCount, String explanation) { }
    public record JobAnalysis(Analysis analysis, Comparison comparison) { }
    public JobAnalysis compare(Long jobId) {
        var job = jobs.findById(jobId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));
        String context = "Title: " + job.getTitle() + "\nRequired skills: " + job.getSkills()
                + "\nExperience level: " + job.getExperienceLevel() + "\nDescription: " + job.getDescription();
        var analysis = analyzeContext(context);
        var match = matcher.match(job.getSkills(), analysis.feedback().skills());
        return new JobAnalysis(analysis, new Comparison(job.getId(), job.getTitle(), match.percentage(), match.matched(),
                match.missing(), match.requiredCount(), "CV skill coverage = matched listed skills / total listed skills × 100, rounded. "
                + "Uses AI-extracted CV skills with case-insensitive exact matching; synonyms may be missed. "
                + "Not a hiring probability or an assessment of overall suitability. No score when the job lists no skills."));
    }
    public Analysis analyze() { return analyzeContext(null); }
    private Analysis analyzeContext(String context) {
        if (!capacity.tryAcquire()) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Another resume analysis is running; retry shortly");
        try {
            // This transactional read finishes before PDF parsing or the model request.
            byte[] content = resumes.downloadOwn();
            var text = extractor.extract(content);
            var feedback = context == null ? ai.analyze(text.text()) : ai.analyze(text.text(), context);
            return new Analysis("ollama", ai.model(), hash(content), text.pages(), feedback,
                    "AI-generated suggestions may be inaccurate. Review them before changing your resume or profile.");
        } finally { capacity.release(); }
    }
    private String hash(byte[] content) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content)); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }
}
