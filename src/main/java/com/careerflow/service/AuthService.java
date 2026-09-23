package com.careerflow.service;

import com.careerflow.dto.request.LoginRequest;
import com.careerflow.dto.request.RegisterRequest;
import com.careerflow.dto.response.CurrentUserResponse;
import com.careerflow.dto.response.LoginResponse;
import com.careerflow.dto.response.RegisterResponse;
import com.careerflow.security.AuthenticatedUser;

public interface AuthService {
    RegisterResponse register(RegisterRequest request);
    LoginResponse login(LoginRequest request);
    AuthenticatedUser authenticatedUser(Long userId);
    CurrentUserResponse currentUser(Long userId);
}
