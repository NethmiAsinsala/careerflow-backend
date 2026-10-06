package com.careerflow.controller;

import com.careerflow.dto.request.PersonalProfileRequest;
import com.careerflow.dto.response.JobSeekerProfileResponse;
import com.careerflow.service.PersonalProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/job-seekers")
@RequiredArgsConstructor
public class PersonalProfileController {
    private final PersonalProfileService service;
    @GetMapping("/me")
    public JobSeekerProfileResponse getMyProfile() { return service.getMyProfile(); }
    @PutMapping("/me")
    public JobSeekerProfileResponse updateMyProfile(@Valid @RequestBody PersonalProfileRequest request) {
        return service.updateMyProfile(request);
    }
    @GetMapping("/{jobSeekerId}")
    public JobSeekerProfileResponse getProfile(@PathVariable Long jobSeekerId) {
        return service.getProfile(jobSeekerId);
    }
}
