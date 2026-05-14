package com.careerflow.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobApplicationResponse {

    private Long id;

    private Long jobId;

    private Long jobSeekerId;

    private String coverLetter;

    private String resumeUrl;

    private String status;

    private LocalDateTime appliedAt;

    private LocalDateTime updatedAt;
}