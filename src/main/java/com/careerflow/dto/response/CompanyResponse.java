package com.careerflow.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyResponse {

    private Long id;

    private Long employerId;

    private String name;

    private String description;

    private String industry;

    private String website;

    private String location;

    private String logoUrl;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}