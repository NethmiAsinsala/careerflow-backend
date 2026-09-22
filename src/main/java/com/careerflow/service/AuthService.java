package com.careerflow.service;

import com.careerflow.dto.request.RegisterRequest;
import com.careerflow.dto.response.RegisterResponse;

public interface AuthService {
    RegisterResponse register(RegisterRequest request);
}
