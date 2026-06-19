package com.careerflow.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobRequest {

    @NotBlank(message = "Job title is required")
    private String title;

    private String description;

    private String location;

    private String employmentType;

    private String experienceLevel;

    private BigDecimal salaryMin;

    private BigDecimal salaryMax;

    private String skills;

    private LocalDateTime applicationDeadline;

    private String status;
}