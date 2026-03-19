package com.careerflow.controller;

import com.careerflow.dto.request.SkillRequest;
import com.careerflow.dto.response.SkillResponse;
import com.careerflow.service.JobSeekersProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/job-seekers")
public class JobSeekerProfileController {

    private final JobSeekersProfileService jobSeekerProfileService;

    public JobSeekerProfileController(
            JobSeekersProfileService jobSeekerProfileService) {
        this.jobSeekerProfileService = jobSeekerProfileService;
    }
    @PostMapping("/{jobSeekerId}/skills")
    @ResponseStatus(HttpStatus.CREATED)
    public SkillResponse addSkill(
            @PathVariable Long jobSeekerId,
            @RequestBody SkillRequest request) {

        return jobSeekerProfileService.addSkill(jobSeekerId, request);
    }
    @GetMapping("/{jobSeekerId}/skills")
    public List<SkillResponse> getSkills(
            @PathVariable Long jobSeekerId) {

        return jobSeekerProfileService.getSkills(jobSeekerId);
    }
    @DeleteMapping("/{jobSeekerId}/skills/{skillId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSkill(
            @PathVariable Long jobSeekerId,
            @PathVariable Long skillId) {

        jobSeekerProfileService.deleteSkill(jobSeekerId, skillId);
    }
}