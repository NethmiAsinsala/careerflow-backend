package com.careerflow.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class EducationRequest {

    @NotBlank @Size(max=255)
    private String institution;

    @NotBlank @Size(max=255)
    private String degree;

    @Size(max=255)
    private String fieldOfStudy;

    private LocalDate startDate;

    private LocalDate endDate;

    @Size(max=10000)
    private String description;
    @AssertTrue(message="End date must not precede start date")
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isDateRangeValid() { return startDate == null || endDate == null || !endDate.isBefore(startDate); }
}