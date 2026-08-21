package com.careerflow.mapper;

import com.careerflow.dto.response.EmployerResponse;
import com.careerflow.entity.Employer;
import org.springframework.stereotype.Component;

@Component
public class EmployerMapper {

    public EmployerResponse toResponse(Employer employer) {

        return EmployerResponse.builder()
                .id(employer.getId())
                .userId(employer.getUser().getId())
                .build();
    }
}