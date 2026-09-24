package com.careerflow.dto.request;

import lombok.*;
import jakarta.validation.constraints.Size;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobApplicationRequest {

    private String coverLetter;

    @Size(max = 500)
    private String resumeUrl;
}