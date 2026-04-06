package com.careerflow.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedJobResponse {

    private Long id;

    private Long jobId;

    private Long jobSeekerId;

    private String jobTitle;

    private String companyName;

    private String location;

    private String employmentType;

    private String experienceLevel;

    private LocalDateTime savedAt;
}