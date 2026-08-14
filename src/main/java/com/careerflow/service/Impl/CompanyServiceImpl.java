package com.careerflow.service.impl;

import com.careerflow.dto.request.CompanyRequest;
import com.careerflow.dto.response.CompanyResponse;
import com.careerflow.entity.Company;
import com.careerflow.entity.Employer;
import com.careerflow.mapper.CompanyMapper;
import com.careerflow.repository.CompanyRepository;
import com.careerflow.repository.EmployerRepository;
import com.careerflow.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;
    private final EmployerRepository employerRepository;
    private final CompanyMapper companyMapper;

    @Override
    public CompanyResponse createCompany(Long employerId, CompanyRequest request) {

        Employer employer = employerRepository.findById(employerId)
                .orElseThrow(() ->
                        new RuntimeException("Employer not found with id: " + employerId));

        if (companyRepository.existsByEmployerId(employerId)) {
            throw new RuntimeException(
                    "Company already exists for employer id: " + employerId
            );
        }

        Company company = companyMapper.toEntity(request);
        company.setEmployer(employer);

        return companyMapper.toResponse(
                companyRepository.save(company)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanyResponse> getAllCompanies() {

        return companyRepository.findAll()
                .stream()
                .map(companyMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyResponse getCompanyById(Long id) {

        Company company = companyRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Company not found with id: " + id));

        return companyMapper.toResponse(company);
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyResponse getCompanyByEmployerId(Long employerId) {

        Company company = companyRepository.findByEmployerId(employerId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Company not found for employer id: " + employerId
                        ));

        return companyMapper.toResponse(company);
    }

    @Override
    public CompanyResponse updateCompany(Long id, CompanyRequest request) {

        Company company = companyRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Company not found with id: " + id));

        companyMapper.updateEntity(company, request);

        return companyMapper.toResponse(
                companyRepository.save(company)
        );
    }

    @Override
    public void deleteCompany(Long id) {

        Company company = companyRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Company not found with id: " + id));

        companyRepository.delete(company);
    }
}