package com.careerflow.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedJobRequest {

    private Long jobId;

    private Long jobSeekerId;
}