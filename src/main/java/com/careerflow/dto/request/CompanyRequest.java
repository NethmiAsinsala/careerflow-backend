package com.careerflow.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyRequest {

    @NotBlank(message = "Company name is required")
    private String name;

    private String description;

    private String industry;

    private String website;

    private String location;

    private String logoUrl;
}