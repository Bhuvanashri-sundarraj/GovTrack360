package com.govtrack360.controller;

import com.govtrack360.dto.attendance.AttendanceMonthlySummaryResponse;
import com.govtrack360.dto.attendance.AttendanceResponse;
import com.govtrack360.dto.attendance.TodayAttendanceSummaryResponse;
import com.govtrack360.security.CustomUserDetails;
import com.govtrack360.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/check-in")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER','EMPLOYEE')")
    public ResponseEntity<AttendanceResponse> checkIn(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(attendanceService.checkIn(userDetails.getUser()));
    }

    @PatchMapping("/check-out")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER','EMPLOYEE')")
    public ResponseEntity<AttendanceResponse> checkOut(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(attendanceService.checkOut(userDetails.getUser()));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER','EMPLOYEE')")
    public ResponseEntity<List<AttendanceResponse>> getMyHistory(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(attendanceService.getMyHistory(userDetails.getUser(), month, year));
    }

    @GetMapping("/my/summary")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER','EMPLOYEE')")
    public ResponseEntity<AttendanceMonthlySummaryResponse> getMySummary(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(attendanceService.getMySummary(userDetails.getUser(), month, year));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER')")
    public ResponseEntity<List<AttendanceResponse>> search(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(attendanceService.search(employeeId, departmentId, date, userDetails.getUser()));
    }

    @GetMapping("/summary/today")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER')")
    public ResponseEntity<TodayAttendanceSummaryResponse> getTodaySummary() {
        return ResponseEntity.ok(attendanceService.getTodaySummary());
    }
}
