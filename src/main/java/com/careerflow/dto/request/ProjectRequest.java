package com.careerflow.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProjectRequest {

    private String name;

    private String description;

    private String technologies;

    private String projectUrl;
}