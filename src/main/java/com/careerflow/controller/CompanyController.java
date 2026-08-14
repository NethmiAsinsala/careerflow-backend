package com.careerflow.controller;

import com.careerflow.dto.request.CompanyRequest;
import com.careerflow.dto.response.CompanyResponse;
import com.careerflow.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @PostMapping("/employer/{employerId}")
    public ResponseEntity<CompanyResponse> createCompany(
            @PathVariable Long employerId,
            @Valid @RequestBody CompanyRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(companyService.createCompany(employerId, request));
    }

    @GetMapping
    public ResponseEntity<List<CompanyResponse>> getAllCompanies() {

        return ResponseEntity.ok(
                companyService.getAllCompanies()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompanyResponse> getCompanyById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                companyService.getCompanyById(id)
        );
    }

    @GetMapping("/employer/{employerId}")
    public ResponseEntity<CompanyResponse> getCompanyByEmployerId(
            @PathVariable Long employerId) {

        return ResponseEntity.ok(
                companyService.getCompanyByEmployerId(employerId)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<CompanyResponse> updateCompany(
            @PathVariable Long id,
            @Valid @RequestBody CompanyRequest request) {

        return ResponseEntity.ok(
                companyService.updateCompany(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCompany(
            @PathVariable Long id) {

        companyService.deleteCompany(id);

        return ResponseEntity.noContent().build();
    }
}