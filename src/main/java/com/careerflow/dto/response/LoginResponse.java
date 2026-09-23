package com.careerflow.dto.response;

public record LoginResponse(String accessToken, String tokenType, long expiresIn,
                            CurrentUserResponse user) {
}
