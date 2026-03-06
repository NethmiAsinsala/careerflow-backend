package com.careerflow.service;

import com.careerflow.dto.request.EducationRequest;
import com.careerflow.dto.request.ExperienceRequest;
import com.careerflow.dto.request.ProjectRequest;
import com.careerflow.dto.request.SkillRequest;
import com.careerflow.dto.response.EducationResponse;
import com.careerflow.dto.response.ExperienceResponse;
import com.careerflow.dto.response.ProjectResponse;
import com.careerflow.dto.response.SkillResponse;

import java.util.List;

public interface JobSeekersProfileService {

    SkillResponse addSkill(Long jobSeekerId, SkillRequest request);

    List<SkillResponse> getSkills(Long jobSeekerId);

    SkillResponse updateSkill(Long jobSeekerId, Long skillId, SkillRequest request);

    void deleteSkill(Long jobSeekerId, Long skillId);

    EducationResponse addEducation(Long jobSeekerId, EducationRequest request);

    EducationResponse updateEducation( Long jobSeekerId, Long educationId, EducationRequest request);

    List<EducationResponse> getEducation(Long jobSeekerId);

    void deleteEducation(Long jobSeekerId, Long educationId);

    ExperienceResponse addExperience(Long jobSeekerId, ExperienceRequest request);

    List<ExperienceResponse> getExperience(Long jobSeekerId);

    void deleteExperience(Long jobSeekerId, Long experienceId);

    ProjectResponse addProject(Long jobSeekerId, ProjectRequest request);

    List<ProjectResponse> getProjects(Long jobSeekerId);

    void deleteProject(Long jobSeekerId, Long projectId);
}