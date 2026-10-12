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
    private final Semaphore capacity = new Semaphore(1);
    public record Analysis(String provider, String model, String resumeSha256, int pages,
                           ResumeFeedback feedback, String note) { }
    public Analysis analyze() {
        if (!capacity.tryAcquire()) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Another resume analysis is running; retry shortly");
        try {
            // This transactional read finishes before PDF parsing or the model request.
            byte[] content = resumes.downloadOwn();
            var text = extractor.extract(content);
            var feedback = ai.analyze(text.text());
            return new Analysis("ollama", ai.model(), hash(content), text.pages(), feedback,
                    "AI-generated suggestions may be inaccurate. Review them before changing your resume or profile.");
        } finally { capacity.release(); }
    }
    private String hash(byte[] content) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content)); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }
}
