package com.careerflow.service.Impl;

import com.careerflow.dto.request.EducationRequest;
import com.careerflow.dto.request.ExperienceRequest;
import com.careerflow.dto.request.ProjectRequest;
import com.careerflow.dto.request.SkillRequest;
import com.careerflow.dto.response.EducationResponse;
import com.careerflow.dto.response.ExperienceResponse;
import com.careerflow.dto.response.ProjectResponse;
import com.careerflow.dto.response.SkillResponse;
import com.careerflow.entity.Education;
import com.careerflow.entity.Experience;
import com.careerflow.entity.JobSeeker;
import com.careerflow.entity.Project;
import com.careerflow.entity.Skill;
import com.careerflow.mapper.JobSeekerProfileMapper;
import com.careerflow.repository.EducationRepository;
import com.careerflow.repository.ExperienceRepository;
import com.careerflow.repository.JobSeekerRepository;
import com.careerflow.repository.ProjectRepository;
import com.careerflow.repository.SkillRepository;
import com.careerflow.service.JobSeekersProfileService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JobSeekersProfileServiceImpl implements JobSeekersProfileService {

    private final JobSeekerRepository jobSeekerRepository;
    private final SkillRepository skillRepository;
    private final EducationRepository educationRepository;
    private final ExperienceRepository experienceRepository;
    private final ProjectRepository projectRepository;
    private final JobSeekerProfileMapper mapper;

    public JobSeekersProfileServiceImpl(
            JobSeekerRepository jobSeekerRepository,
            SkillRepository skillRepository,
            EducationRepository educationRepository,
            ExperienceRepository experienceRepository,
            ProjectRepository projectRepository,
            JobSeekerProfileMapper mapper) {

        this.jobSeekerRepository = jobSeekerRepository;
        this.skillRepository = skillRepository;
        this.educationRepository = educationRepository;
        this.experienceRepository = experienceRepository;
        this.projectRepository = projectRepository;
        this.mapper = mapper;
    }

    @Override
    public SkillResponse addSkill(Long jobSeekerId, SkillRequest request) {
        JobSeeker jobSeeker = getJobSeeker(jobSeekerId);

        Skill skill = mapper.toSkillEntity(request);
        skill.setJobSeeker(jobSeeker);

        return mapper.toSkillResponse(skillRepository.save(skill));
    }
    @Override
    public SkillResponse updateSkill(
            Long jobSeekerId,
            Long skillId,
            SkillRequest request) {

        getJobSeeker(jobSeekerId);

        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new RuntimeException("Skill not found"));

        if (!skill.getJobSeeker().getId().equals(jobSeekerId)) {
            throw new RuntimeException("Skill does not belong to this job seeker");
        }

        skill.setName(request.getName());

        return mapper.toSkillResponse(skillRepository.save(skill));
    }

    @Override
    public List<SkillResponse> getSkills(Long jobSeekerId) {
        getJobSeeker(jobSeekerId);

        return skillRepository.findAll()
                .stream()
                .filter(skill -> skill.getJobSeeker().getId().equals(jobSeekerId))
                .map(mapper::toSkillResponse)
                .toList();
    }

    @Override
    public void deleteSkill(Long jobSeekerId, Long skillId) {
        getJobSeeker(jobSeekerId);

        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new RuntimeException("Skill not found"));

        if (!skill.getJobSeeker().getId().equals(jobSeekerId)) {
            throw new RuntimeException("Skill does not belong to this job seeker");
        }

        skillRepository.delete(skill);
    }

    @Override
    public EducationResponse addEducation(Long jobSeekerId, EducationRequest request) {
        JobSeeker jobSeeker = getJobSeeker(jobSeekerId);

        Education education = mapper.toEducationEntity(request);
        education.setJobSeeker(jobSeeker);

        return mapper.toEducationResponse(educationRepository.save(education));
    }
    @Override
    public EducationResponse updateEducation(
            Long jobSeekerId,
            Long educationId,
            EducationRequest request) {

        getJobSeeker(jobSeekerId);

        Education education = educationRepository.findById(educationId)
                .orElseThrow(() -> new RuntimeException("Education not found"));

        if (!education.getJobSeeker().getId().equals(jobSeekerId)) {
            throw new RuntimeException("Education does not belong to this job seeker");
        }

        education.setInstitution(request.getInstitution());
        education.setDegree(request.getDegree());
        education.setFieldOfStudy(request.getFieldOfStudy());
        education.setStartDate(request.getStartDate());
        education.setEndDate(request.getEndDate());
        education.setDescription(request.getDescription());

        return mapper.toEducationResponse(
                educationRepository.save(education));
    }

    @Override
    public List<EducationResponse> getEducation(Long jobSeekerId) {
        getJobSeeker(jobSeekerId);

        return educationRepository.findAll()
                .stream()
                .filter(education -> education.getJobSeeker().getId().equals(jobSeekerId))
                .map(mapper::toEducationResponse)
                .toList();
    }

    @Override
    public void deleteEducation(Long jobSeekerId, Long educationId) {
        getJobSeeker(jobSeekerId);

        Education education = educationRepository.findById(educationId)
                .orElseThrow(() -> new RuntimeException("Education not found"));

        if (!education.getJobSeeker().getId().equals(jobSeekerId)) {
            throw new RuntimeException("Education does not belong to this job seeker");
        }

        educationRepository.delete(education);
    }

    @Override
    public ExperienceResponse addExperience(Long jobSeekerId, ExperienceRequest request) {
        JobSeeker jobSeeker = getJobSeeker(jobSeekerId);

        Experience experience = mapper.toExperienceEntity(request);
        experience.setJobSeeker(jobSeeker);

        return mapper.toExperienceResponse(experienceRepository.save(experience));
    }

    @Override
    public List<ExperienceResponse> getExperience(Long jobSeekerId) {
        getJobSeeker(jobSeekerId);

        return experienceRepository.findAll()
                .stream()
                .filter(experience -> experience.getJobSeeker().getId().equals(jobSeekerId))
                .map(mapper::toExperienceResponse)
                .toList();
    }

    @Override
    public void deleteExperience(Long jobSeekerId, Long experienceId) {
        getJobSeeker(jobSeekerId);

        Experience experience = experienceRepository.findById(experienceId)
                .orElseThrow(() -> new RuntimeException("Experience not found"));

        if (!experience.getJobSeeker().getId().equals(jobSeekerId)) {
            throw new RuntimeException("Experience does not belong to this job seeker");
        }

        experienceRepository.delete(experience);
    }

    @Override
    public ProjectResponse addProject(Long jobSeekerId, ProjectRequest request) {
        JobSeeker jobSeeker = getJobSeeker(jobSeekerId);

        Project project = mapper.toProjectEntity(request);
        project.setJobSeeker(jobSeeker);

        return mapper.toProjectResponse(projectRepository.save(project));
    }

    @Override
    public List<ProjectResponse> getProjects(Long jobSeekerId) {
        getJobSeeker(jobSeekerId);

        return projectRepository.findAll()
                .stream()
                .filter(project -> project.getJobSeeker().getId().equals(jobSeekerId))
                .map(mapper::toProjectResponse)
                .toList();
    }

    @Override
    public void deleteProject(Long jobSeekerId, Long projectId) {
        getJobSeeker(jobSeekerId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if (!project.getJobSeeker().getId().equals(jobSeekerId)) {
            throw new RuntimeException("Project does not belong to this job seeker");
        }

        projectRepository.delete(project);
    }

    private JobSeeker getJobSeeker(Long jobSeekerId) {
        return jobSeekerRepository.findById(jobSeekerId)
                .orElseThrow(() -> new RuntimeException("Job seeker not found"));
    }
}