package com.careerflow.service.impl;

import com.careerflow.dto.request.RegisterRequest;
import com.careerflow.dto.response.RegisterResponse;
import com.careerflow.entity.Employer;
import com.careerflow.entity.JobSeeker;
import com.careerflow.entity.Role;
import com.careerflow.entity.User;
import com.careerflow.repository.*;
import com.careerflow.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JobSeekerRepository jobSeekerRepository;
    private final EmployerRepository employerRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String roleName = request.getRole();
        if (!"JOB_SEEKER".equals(roleName) && !"EMPLOYER".equals(roleName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Role must be JOB_SEEKER or EMPLOYER");
        }
        // BCrypt limits passwords by UTF-8 bytes, not Java character count.
        if (request.getPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Password must not exceed 72 UTF-8 bytes");
        }
        String email = request.getEmail().strip().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Registration role is not configured"));

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        user = userRepository.saveAndFlush(user);

        Long profileId;
        if ("JOB_SEEKER".equals(roleName)) {
            JobSeeker profile = new JobSeeker();
            profile.setUser(user);
            profileId = jobSeekerRepository.save(profile).getId();
        } else {
            Employer profile = Employer.builder().user(user).build();
            profileId = employerRepository.save(profile).getId();
        }
        return new RegisterResponse(user.getId(), user.getEmail(), roleName, profileId);
    }
}
