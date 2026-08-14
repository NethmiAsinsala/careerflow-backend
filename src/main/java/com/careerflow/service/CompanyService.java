package com.careerflow.service;

import com.careerflow.dto.request.CompanyRequest;
import com.careerflow.dto.response.CompanyResponse;

import java.util.List;

public interface CompanyService {

    CompanyResponse createCompany(Long employerId, CompanyRequest request);

    List<CompanyResponse> getAllCompanies();

    CompanyResponse getCompanyById(Long id);

    CompanyResponse getCompanyByEmployerId(Long employerId);

    CompanyResponse updateCompany(Long id, CompanyRequest request);

    void deleteCompany(Long id);
}