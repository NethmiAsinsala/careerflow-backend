package com.careerflow.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedJobRequest {

    @NotNull @Positive
    private Long jobId;

    @NotNull @Positive
    private Long jobSeekerId;
}