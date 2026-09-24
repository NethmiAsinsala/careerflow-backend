package com.careerflow.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
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
    @Size(max = 255)
    private String title;

    private String description;

    @Size(max = 255)
    private String location;

    @Size(max = 100)
    private String employmentType;

    @Size(max = 100)
    private String experienceLevel;

    @DecimalMin(value = "0", message = "Salary must not be negative")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal salaryMin;

    @DecimalMin(value = "0", message = "Salary must not be negative")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal salaryMax;

    private String skills;

    private LocalDateTime applicationDeadline;

    private String status;
}