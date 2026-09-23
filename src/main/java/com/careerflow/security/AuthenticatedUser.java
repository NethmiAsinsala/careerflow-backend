package com.careerflow.security;

public record AuthenticatedUser(Long id, String email, String role) {
}
