package com.careerflow.service.impl;

import com.careerflow.dto.request.PersonalProfileRequest;
import com.careerflow.dto.response.JobSeekerProfileResponse;
import com.careerflow.entity.JobSeeker;
import com.careerflow.repository.*;
import com.careerflow.mapper.JobSeekerProfileMapper;
import com.careerflow.security.AuthenticatedUser;
import com.careerflow.service.PersonalProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
@PreAuthorize("denyAll()")
public class PersonalProfileServiceImpl implements PersonalProfileService {
    private final JobSeekerRepository seekers;
    private final SkillRepository skills;
    private final EducationRepository education;
    private final ExperienceRepository experience;
    private final ProjectRepository projects;
    private final JobSeekerProfileMapper mapper;

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('JOB_SEEKER')")
    public JobSeekerProfileResponse getMyProfile() { return response(myProfile()); }

    @Override
    @PreAuthorize("hasRole('JOB_SEEKER')")
    public JobSeekerProfileResponse updateMyProfile(PersonalProfileRequest request) {
        JobSeeker profile = myProfile();
        profile.setFirstName(clean(request.getFirstName()));
        profile.setLastName(clean(request.getLastName()));
        profile.setPhone(clean(request.getPhone()));
        profile.setHeadline(clean(request.getHeadline()));
        profile.setSummary(clean(request.getSummary()));
        profile.setLocation(clean(request.getLocation()));
        profile.setLinkedinUrl(clean(request.getLinkedinUrl()));
        profile.setGithubUrl(clean(request.getGithubUrl()));
        profile.setPortfolioUrl(clean(request.getPortfolioUrl()));
        return response(seekers.save(profile));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("@resourceAccess.readsJobSeeker(#jobSeekerId)")
    public JobSeekerProfileResponse getProfile(Long jobSeekerId) {
        return response(seekers.findById(jobSeekerId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Job seeker profile not found")));
    }

    private JobSeeker myProfile() {
        var user = (AuthenticatedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return seekers.findByUserId(user.id()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Job seeker profile not found"));
    }

    private String clean(String value) { return value == null || value.isBlank() ? null : value.strip(); }

    private JobSeekerProfileResponse response(JobSeeker profile) {
        Long id = profile.getId();
        return new JobSeekerProfileResponse(id, profile.getUser().getId(), profile.getUser().getEmail(),
                profile.getFirstName(), profile.getLastName(), profile.getPhone(), profile.getHeadline(),
                profile.getSummary(), profile.getLocation(), profile.getLinkedinUrl(), profile.getGithubUrl(), profile.getPortfolioUrl(),
                skills.findByJobSeekerIdOrderByIdAsc(id).stream().map(mapper::toSkillResponse).toList(),
                education.findByJobSeekerIdOrderByIdAsc(id).stream().map(mapper::toEducationResponse).toList(),
                experience.findByJobSeekerIdOrderByIdAsc(id).stream().map(mapper::toExperienceResponse).toList(),
                projects.findByJobSeekerIdOrderByIdAsc(id).stream().map(mapper::toProjectResponse).toList());
    }
}
