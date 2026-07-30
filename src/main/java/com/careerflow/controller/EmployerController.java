package com.careerflow.controller;

import com.careerflow.dto.request.EmployerRequest;
import com.careerflow.dto.response.EmployerResponse;
import com.careerflow.service.EmployerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employers")
@RequiredArgsConstructor
public class EmployerController {

    private final EmployerService employerService;

    @PostMapping
    public ResponseEntity<EmployerResponse> createEmployer(
            @Valid @RequestBody EmployerRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(employerService.createEmployer(request));
    }

    @GetMapping
    public ResponseEntity<List<EmployerResponse>> getAllEmployers() {

        return ResponseEntity.ok(
                employerService.getAllEmployers()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployerResponse> getEmployerById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                employerService.getEmployerById(id)
        );
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<EmployerResponse> getEmployerByUserId(
            @PathVariable Long userId) {

        return ResponseEntity.ok(
                employerService.getEmployerByUserId(userId)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployer(
            @PathVariable Long id) {

        employerService.deleteEmployer(id);

        return ResponseEntity.noContent().build();
    }
}