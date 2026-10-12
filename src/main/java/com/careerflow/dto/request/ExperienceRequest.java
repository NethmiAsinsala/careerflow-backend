package com.careerflow.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ExperienceRequest {

    @NotBlank @Size(max=255)
    private String companyName;

    @NotBlank @Size(max=255)
    private String jobTitle;

    private LocalDate startDate;

    private LocalDate endDate;

    @Size(max=10000)
    private String description;
    @AssertTrue(message="End date must not precede start date")
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isDateRangeValid() { return startDate == null || endDate == null || !endDate.isBefore(startDate); }
}