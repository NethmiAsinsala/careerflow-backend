package com.careerflow.service.impl;

import com.careerflow.dto.request.RegisterRequest;
import com.careerflow.dto.request.LoginRequest;
import com.careerflow.dto.response.CurrentUserResponse;
import com.careerflow.dto.response.LoginResponse;
import com.careerflow.security.AuthenticatedUser;
import com.careerflow.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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
    private final JwtService jwtService;
    private final String dummyHash = new BCryptPasswordEncoder().encode(java.util.UUID.randomUUID().toString());

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        if (request.getPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw invalidCredentials();
        }
        String email = request.getEmail().strip().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmail(email).orElse(null);
        // Perform a hash check even for unknown accounts; return the same error for all failures.
        boolean matches = passwordEncoder.matches(request.getPassword(), user == null ? dummyHash : user.getPassword());
        if (user == null || !matches || !Boolean.TRUE.equals(user.getActive())) {
            throw invalidCredentials();
        }
        return new LoginResponse(jwtService.issue(user.getId()), "Bearer", jwtService.expirationSeconds(),
                toCurrentUser(user));
    }

    @Override
    @Transactional(readOnly = true)
    public AuthenticatedUser authenticatedUser(Long userId) {
        User user = activeUser(userId);
        // Read the current role from the database so account changes take effect immediately.
        return new AuthenticatedUser(user.getId(), user.getEmail(), user.getRole().getName());
    }

    @Override
    @Transactional(readOnly = true)
    public CurrentUserResponse currentUser(Long userId) {
        return toCurrentUser(activeUser(userId));
    }

    private User activeUser(Long userId) {
        return userRepository.findById(userId).filter(user -> Boolean.TRUE.equals(user.getActive()))
                .orElseThrow(this::invalidCredentials);
    }

    private CurrentUserResponse toCurrentUser(User user) {
        String role = user.getRole().getName();
        Long profileId = switch (role) {
            case "JOB_SEEKER" -> jobSeekerRepository.findByUserId(user.getId()).map(JobSeeker::getId).orElse(null);
            case "EMPLOYER" -> employerRepository.findByUserId(user.getId()).map(Employer::getId).orElse(null);
            default -> null;
        };
        return new CurrentUserResponse(user.getId(), user.getEmail(), role, profileId);
    }

    private BadCredentialsException invalidCredentials() {
        return new BadCredentialsException("Invalid email or password");
    }


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
