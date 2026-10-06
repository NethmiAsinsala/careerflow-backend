package com.careerflow.dto.request;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
@Getter @Setter
public class AdminUserPageRequest {
 @Min(0) @Max(1000000) private int page = 0;
 @Min(1) @Max(100) private int size = 20;
}
