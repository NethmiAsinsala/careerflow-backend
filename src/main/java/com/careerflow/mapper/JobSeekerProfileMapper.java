package com.careerflow.mapper;

import com.careerflow.dto.request.ExperienceRequest;
import com.careerflow.dto.request.ProjectRequest;
import com.careerflow.dto.request.SkillRequest;
import com.careerflow.dto.request.EducationRequest;
import com.careerflow.dto.response.EducationResponse;
import com.careerflow.dto.response.ExperienceResponse;
import com.careerflow.dto.response.ProjectResponse;
import com.careerflow.dto.response.SkillResponse;
import com.careerflow.entity.Education;
import com.careerflow.entity.Experience;
import com.careerflow.entity.Project;
import com.careerflow.entity.Skill;
import org.springframework.stereotype.Component;

@Component
public class JobSeekerProfileMapper {

    // ==================== Skill ====================

    public Skill toSkillEntity(SkillRequest request) {
        Skill skill = new Skill();
        skill.setName(request.getName());

        return skill;
    }

    public SkillResponse toSkillResponse(Skill skill) {
        SkillResponse response = new SkillResponse();
        response.setId(skill.getId());
        response.setName(skill.getName());

        return response;
    }

    // ==================== Education ====================

    public Education toEducationEntity(EducationRequest request) {
        Education education = new Education();

        education.setInstitution(request.getInstitution());
        education.setDegree(request.getDegree());
        education.setFieldOfStudy(request.getFieldOfStudy());
        education.setStartDate(request.getStartDate());
        education.setEndDate(request.getEndDate());
        education.setDescription(request.getDescription());

        return education;
    }

    public EducationResponse toEducationResponse(Education education) {
        EducationResponse response = new EducationResponse();

        response.setId(education.getId());
        response.setInstitution(education.getInstitution());
        response.setDegree(education.getDegree());
        response.setFieldOfStudy(education.getFieldOfStudy());
        response.setStartDate(education.getStartDate());
        response.setEndDate(education.getEndDate());
        response.setDescription(education.getDescription());

        return response;
    }
    
    // ==================== Experience ====================

    public Experience toExperienceEntity(ExperienceRequest request) {
        Experience experience = new Experience();

        experience.setCompanyName(request.getCompanyName());
        experience.setJobTitle(request.getJobTitle());
        experience.setStartDate(request.getStartDate());
        experience.setEndDate(request.getEndDate());
        experience.setDescription(request.getDescription());

        return experience;
    }

    public ExperienceResponse toExperienceResponse(Experience experience) {
        ExperienceResponse response = new ExperienceResponse();

        response.setId(experience.getId());
        response.setCompanyName(experience.getCompanyName());
        response.setJobTitle(experience.getJobTitle());
        response.setStartDate(experience.getStartDate());
        response.setEndDate(experience.getEndDate());
        response.setDescription(experience.getDescription());

        return response;
    }

    // ==================== Project ====================

    public Project toProjectEntity(ProjectRequest request) {
        Project project = new Project();

        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setTechnologies(request.getTechnologies());
        project.setProjectUrl(request.getProjectUrl());

        return project;
    }

    public ProjectResponse toProjectResponse(Project project) {
        ProjectResponse response = new ProjectResponse();

        response.setId(project.getId());
        response.setName(project.getName());
        response.setDescription(project.getDescription());
        response.setTechnologies(project.getTechnologies());
        response.setProjectUrl(project.getProjectUrl());

        return response;
    }
}