package com.careerflow.service;

import com.careerflow.dto.request.EmployerRequest;
import com.careerflow.dto.response.EmployerResponse;

import java.util.List;

public interface EmployerService {

    EmployerResponse createEmployer(EmployerRequest request);

    List<EmployerResponse> getAllEmployers();

    EmployerResponse getEmployerById(Long id);

    EmployerResponse getEmployerByUserId(Long userId);

    void deleteEmployer(Long id);
}