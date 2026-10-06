package com.careerflow.controller;

import com.careerflow.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService dashboards;
    @GetMapping("/candidate")
    public DashboardService.CandidateDashboard candidate() { return dashboards.candidate(); }
    @GetMapping("/employer")
    public DashboardService.EmployerDashboard employer() { return dashboards.employer(); }
    @GetMapping("/admin")
    public DashboardService.AdminDashboard admin() { return dashboards.admin(); }
}
