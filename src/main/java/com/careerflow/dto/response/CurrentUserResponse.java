package com.careerflow.dto.response;

public record CurrentUserResponse(Long userId, String email, String role, Long profileId) {
}
