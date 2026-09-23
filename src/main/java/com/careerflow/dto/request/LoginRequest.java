package com.careerflow.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {
    @NotBlank @Email @Size(max = 255)
    private String email;
    @NotBlank @Size(max = 72)
    private String password;
}
