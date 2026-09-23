package com.careerflow.controller;

import com.careerflow.dto.request.LoginRequest;
import com.careerflow.dto.request.RegisterRequest;
import com.careerflow.dto.response.CurrentUserResponse;
import com.careerflow.dto.response.LoginResponse;
import com.careerflow.dto.response.RegisterResponse;
import com.careerflow.security.AuthenticatedUser;
import com.careerflow.service.AuthService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    @SecurityRequirements
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    @SecurityRequirements
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public CurrentUserResponse me(@AuthenticationPrincipal AuthenticatedUser user) {
        return authService.currentUser(user.id());
    }
}
