package com.govtrack360.controller;

import com.govtrack360.dto.performance.*;
import com.govtrack360.security.CustomUserDetails;
import com.govtrack360.service.PerformanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/performance")
@RequiredArgsConstructor
public class PerformanceController {

    private final PerformanceService performanceService;

    // ---- Self-service (any signed-in user with an employee record) ----

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER','EMPLOYEE')")
    public ResponseEntity<PerformanceScoreResponse> getMyScore(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(performanceService.getMyScore(userDetails.getUser(), from, to));
    }

    @GetMapping("/my/trend")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER','EMPLOYEE')")
    public ResponseEntity<List<PerformanceTrendPoint>> getMyTrend(
            @RequestParam(required = false) Integer months,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(performanceService.getMyTrend(userDetails.getUser(), months));
    }

    @GetMapping("/reviews/my")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER','EMPLOYEE')")
    public ResponseEntity<List<PerformanceReviewResponse>> getMyReviews(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(performanceService.getMyReviews(userDetails.getUser()));
    }

    // ---- Management (ADMIN full, OFFICER department-scoped) ----

    @GetMapping("/scope")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER')")
    public ResponseEntity<ScopeResponse> getScope(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(performanceService.getScope(userDetails.getUser()));
    }

    @GetMapping("/eligible-employees")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER')")
    public ResponseEntity<List<EligibleEmployeeResponse>> getEligibleEmployees(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(performanceService.getEligibleEmployees(userDetails.getUser()));
    }

    @GetMapping("/leaderboard")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER')")
    public ResponseEntity<List<PerformanceScoreResponse>> getLeaderboard(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(performanceService.getLeaderboard(userDetails.getUser(), departmentId, from, to));
    }

    @GetMapping("/employees/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER')")
    public ResponseEntity<PerformanceScoreResponse> getEmployeeScore(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(performanceService.getEmployeeScore(id, userDetails.getUser(), from, to));
    }

    @GetMapping("/employees/{id}/trend")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER')")
    public ResponseEntity<List<PerformanceTrendPoint>> getEmployeeTrend(
            @PathVariable Long id,
            @RequestParam(required = false) Integer months,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(performanceService.getEmployeeTrend(id, userDetails.getUser(), months));
    }

    // ---- Reviews ----

    @GetMapping("/reviews")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER')")
    public ResponseEntity<List<PerformanceReviewResponse>> searchReviews(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Long departmentId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(performanceService.searchReviews(userDetails.getUser(), employeeId, departmentId));
    }

    @GetMapping("/reviews/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER','EMPLOYEE')")
    public ResponseEntity<PerformanceReviewResponse> getReview(@PathVariable Long id,
                                                                @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(performanceService.getReview(id, userDetails.getUser()));
    }

    @PostMapping("/reviews")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER')")
    public ResponseEntity<PerformanceReviewResponse> createReview(@Valid @RequestBody PerformanceReviewRequest request,
                                                                   @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(performanceService.createReview(request, userDetails.getUser()));
    }

    @PutMapping("/reviews/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER')")
    public ResponseEntity<PerformanceReviewResponse> updateReview(@PathVariable Long id,
                                                                   @Valid @RequestBody PerformanceReviewUpdateRequest request,
                                                                   @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(performanceService.updateReview(id, request, userDetails.getUser()));
    }

    @DeleteMapping("/reviews/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id,
                                              @AuthenticationPrincipal CustomUserDetails userDetails) {
        performanceService.deleteReview(id, userDetails.getUser());
        return ResponseEntity.noContent().build();
    }
}
