package com.careerflow.service;

import com.careerflow.dto.response.JobMatchResponse;
import com.careerflow.entity.Skill;
import com.careerflow.repository.*;
import com.careerflow.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class JobMatchService {
    private final JobRepository jobs;
    private final JobSeekerRepository seekers;
    private final SkillRepository skills;
    private final SkillMatcher matcher;

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('JOB_SEEKER')")
    public JobMatchResponse match(Long jobId) {
        var user = (AuthenticatedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        var profile = seekers.findByUserId(user.id()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Job seeker profile not found"));
        var job = jobs.findById(jobId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));
        var result = matcher.match(job.getSkills(), skills.findByJobSeekerIdOrderByIdAsc(profile.getId())
                .stream().map(Skill::getName).toList());
        return new JobMatchResponse(jobId, profile.getId(), result.percentage(), result.matched(), result.missing(),
                result.requiredCount(), result.requiredCount() == 0 ? "This job has no specified required skills" :
                "Skill coverage only; this score does not measure hiring likelihood");
    }
}
