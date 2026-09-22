package com.careerflow.dto.response;

public record RegisterResponse(Long userId, String email, String role, Long profileId) {
}
