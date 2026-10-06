package com.careerflow.service;

import com.careerflow.dto.request.PersonalProfileRequest;
import com.careerflow.dto.response.JobSeekerProfileResponse;

public interface PersonalProfileService {
    JobSeekerProfileResponse getMyProfile();
    JobSeekerProfileResponse updateMyProfile(PersonalProfileRequest request);
    JobSeekerProfileResponse getProfile(Long jobSeekerId);
}
