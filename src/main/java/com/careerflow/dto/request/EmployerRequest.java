package com.careerflow.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployerRequest {

    @NotNull(message = "User ID is required")
    private Long userId;
}