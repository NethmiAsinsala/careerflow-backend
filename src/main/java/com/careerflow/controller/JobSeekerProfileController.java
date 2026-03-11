package com.careerflow.controller;

import com.careerflow.dto.request.EducationRequest;
import com.careerflow.dto.request.ExperienceRequest;
import com.careerflow.dto.request.ProjectRequest;
import com.careerflow.dto.request.SkillRequest;
import com.careerflow.dto.response.EducationResponse;
import com.careerflow.dto.response.ExperienceResponse;
import com.careerflow.dto.response.ProjectResponse;
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
    @PostMapping("/{jobSeekerId}/education")
    @ResponseStatus(HttpStatus.CREATED)
    public EducationResponse addEducation(
            @PathVariable Long jobSeekerId,
            @RequestBody EducationRequest request) {

        return jobSeekerProfileService.addEducation(jobSeekerId, request);
    }
    @GetMapping("/{jobSeekerId}/education")
    public List<EducationResponse> getEducation(
            @PathVariable Long jobSeekerId) {

        return jobSeekerProfileService.getEducation(jobSeekerId);
    }
    @DeleteMapping("/{jobSeekerId}/education/{educationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEducation(
            @PathVariable Long jobSeekerId,
            @PathVariable Long educationId) {

        jobSeekerProfileService.deleteEducation(jobSeekerId, educationId);
    }
    @PostMapping("/{jobSeekerId}/experience")
    @ResponseStatus(HttpStatus.CREATED)
    public ExperienceResponse addExperience(
            @PathVariable Long jobSeekerId,
            @RequestBody ExperienceRequest request) {

        return jobSeekerProfileService.addExperience(jobSeekerId, request);
    }
    @GetMapping("/{jobSeekerId}/experience")
    public List<ExperienceResponse> getExperience(
            @PathVariable Long jobSeekerId) {

        return jobSeekerProfileService.getExperience(jobSeekerId);
    }
    @DeleteMapping("/{jobSeekerId}/experience/{experienceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExperience(
            @PathVariable Long jobSeekerId,
            @PathVariable Long experienceId) {

        jobSeekerProfileService.deleteExperience(jobSeekerId, experienceId);
    }
    @PostMapping("/{jobSeekerId}/projects")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse addProject(
            @PathVariable Long jobSeekerId,
            @RequestBody ProjectRequest request) {

        return jobSeekerProfileService.addProject(jobSeekerId, request);
    }
    @GetMapping("/{jobSeekerId}/projects")
    public List<ProjectResponse> getProjects(
            @PathVariable Long jobSeekerId) {

        return jobSeekerProfileService.getProjects(jobSeekerId);
    }
    @DeleteMapping("/{jobSeekerId}/projects/{projectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProject(
            @PathVariable Long jobSeekerId,
            @PathVariable Long projectId) {

        jobSeekerProfileService.deleteProject(jobSeekerId, projectId);
    }

}