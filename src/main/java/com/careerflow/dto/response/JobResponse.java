package com.careerflow.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobResponse {

    private Long id;

    private Long employerId;

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

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}