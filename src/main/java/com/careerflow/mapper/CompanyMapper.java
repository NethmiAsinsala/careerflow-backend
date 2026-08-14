package com.careerflow.mapper;

import com.careerflow.dto.request.CompanyRequest;
import com.careerflow.dto.response.CompanyResponse;
import com.careerflow.entity.Company;
import org.springframework.stereotype.Component;

@Component
public class CompanyMapper {

    public Company toEntity(CompanyRequest request) {
        return Company.builder()
                .name(request.getName())
                .description(request.getDescription())
                .industry(request.getIndustry())
                .website(request.getWebsite())
                .location(request.getLocation())
                .logoUrl(request.getLogoUrl())
                .build();
    }

    public CompanyResponse toResponse(Company company) {
        return CompanyResponse.builder()
                .id(company.getId())
                .employerId(company.getEmployer().getId())
                .name(company.getName())
                .description(company.getDescription())
                .industry(company.getIndustry())
                .website(company.getWebsite())
                .location(company.getLocation())
                .logoUrl(company.getLogoUrl())
                .createdAt(company.getCreatedAt())
                .updatedAt(company.getUpdatedAt())
                .build();
    }

    public void updateEntity(Company company, CompanyRequest request) {
        company.setName(request.getName());
        company.setDescription(request.getDescription());
        company.setIndustry(request.getIndustry());
        company.setWebsite(request.getWebsite());
        company.setLocation(request.getLocation());
        company.setLogoUrl(request.getLogoUrl());
    }
}