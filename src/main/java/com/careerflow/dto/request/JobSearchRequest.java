package com.careerflow.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class JobSearchRequest {
    @Size(max = 255) private String keyword;
    @Size(max = 255) private String location;
    @Size(max = 100) private String skill;
    @Size(max = 100) private String employmentType;
    @Size(max = 100) private String experienceLevel;
    @Positive private Long employerId;
    @Pattern(regexp = "OPEN|CLOSED", message = "Status must be OPEN or CLOSED")
    private String status;
    @DecimalMin("0") @Digits(integer = 10, fraction = 2) private BigDecimal salaryMin;
    @DecimalMin("0") @Digits(integer = 10, fraction = 2) private BigDecimal salaryMax;
    private boolean availableOnly = false;
    @Min(0) @Max(1000000) private int page = 0;
    @Min(1) @Max(100) private int size = 20;
    @NotBlank @Size(max = 60) private String sort = "createdAt,desc";
}
