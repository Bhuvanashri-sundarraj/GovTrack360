package com.govtrack360.controller;

import com.govtrack360.dto.dashboard.AdminDashboardResponse;
import com.govtrack360.dto.dashboard.EmployeeDashboardResponse;
import com.govtrack360.dto.dashboard.OfficerDashboardResponse;
import com.govtrack360.security.CustomUserDetails;
import com.govtrack360.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminDashboardResponse> getAdminDashboard() {
        return ResponseEntity.ok(dashboardService.getAdminDashboard());
    }

    @GetMapping("/officer")
    @PreAuthorize("hasRole('OFFICER')")
    public ResponseEntity<OfficerDashboardResponse> getOfficerDashboard(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(dashboardService.getOfficerDashboard(userDetails.getUser()));
    }

    @GetMapping("/employee")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<EmployeeDashboardResponse> getEmployeeDashboard(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(dashboardService.getEmployeeDashboard(userDetails.getUser()));
    }
}
