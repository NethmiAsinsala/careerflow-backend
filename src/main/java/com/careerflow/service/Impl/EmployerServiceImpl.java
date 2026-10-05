package com.careerflow.service.impl;

import com.careerflow.dto.request.EmployerRequest;
import com.careerflow.dto.response.EmployerResponse;
import com.careerflow.entity.Employer;
import com.careerflow.entity.User;
import com.careerflow.mapper.EmployerMapper;
import com.careerflow.repository.EmployerRepository;
import com.careerflow.repository.UserRepository;
import com.careerflow.service.EmployerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@PreAuthorize("denyAll()")
@RequiredArgsConstructor
@Transactional
public class EmployerServiceImpl implements EmployerService {

    private final EmployerRepository employerRepository;
    private final UserRepository userRepository;
    private final EmployerMapper employerMapper;

    @Override
    @PreAuthorize("@resourceAccess.isAdmin()")
    public EmployerResponse createEmployer(EmployerRequest request) {

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found with id: " + request.getUserId()
                        ));

        if (employerRepository.existsByUserId(request.getUserId())) {
            throw new RuntimeException(
                    "Employer already exists for user id: " + request.getUserId()
            );
        }

        Employer employer = Employer.builder()
                .user(user)
                .build();

        return employerMapper.toResponse(
                employerRepository.save(employer)
        );
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("@resourceAccess.isAdmin()")
    public List<EmployerResponse> getAllEmployers() {

        return employerRepository.findAll()
                .stream()
                .map(employerMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("@resourceAccess.managesEmployer(#id)")
    public EmployerResponse getEmployerById(Long id) {

        Employer employer = employerRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Employer not found with id: " + id
                        ));

        return employerMapper.toResponse(employer);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("@resourceAccess.readsEmployerUser(#userId)")
    public EmployerResponse getEmployerByUserId(Long userId) {

        Employer employer = employerRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Employer not found for user id: " + userId
                        ));

        return employerMapper.toResponse(employer);
    }

    @Override
    @PreAuthorize("@resourceAccess.isAdmin()")
    public void deleteEmployer(Long id) {

        Employer employer = employerRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Employer not found with id: " + id
                        ));

        employerRepository.delete(employer);
    }
}