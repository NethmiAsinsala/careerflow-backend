package com.careerflow.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ExperienceRequest {

    private String companyName;

    private String jobTitle;

    private LocalDate startDate;

    private LocalDate endDate;

    private String description;
}