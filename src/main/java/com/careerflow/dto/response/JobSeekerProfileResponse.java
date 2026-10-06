package com.careerflow.dto.response;

import java.util.List;

public record JobSeekerProfileResponse(Long id, Long userId, String email,
        String firstName, String lastName, String phone, String headline, String summary,
        String location, String linkedinUrl, String githubUrl, String portfolioUrl,
        List<SkillResponse> skills, List<EducationResponse> education,
        List<ExperienceResponse> experience, List<ProjectResponse> projects) {
}
