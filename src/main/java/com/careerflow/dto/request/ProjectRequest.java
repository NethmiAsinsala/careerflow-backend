package com.careerflow.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProjectRequest {

    @NotBlank @Size(max=255)
    private String name;

    @Size(max=10000)
    private String description;

    @Size(max=10000)
    private String technologies;

    @com.careerflow.validation.WebUrl @Size(max=500)
    private String projectUrl;
}