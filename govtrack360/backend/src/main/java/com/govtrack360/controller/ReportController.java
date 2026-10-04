package com.govtrack360.controller;

import com.govtrack360.dto.report.ReportResponse;
import com.govtrack360.dto.report.ReportType;
import com.govtrack360.dto.report.ReportTypeInfo;
import com.govtrack360.exception.InvalidRequestException;
import com.govtrack360.security.CustomUserDetails;
import com.govtrack360.service.DateRange;
import com.govtrack360.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','OFFICER')")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/types")
    public ResponseEntity<List<ReportTypeInfo>> getTypes() {
        return ResponseEntity.ok(reportService.getTypes());
    }

    @GetMapping("/{type}")
    public ResponseEntity<ReportResponse> generate(
            @PathVariable String type,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(reportService.generate(parseType(type), departmentId, from, to, userDetails.getUser()));
    }

    @GetMapping("/{type}/export")
    public ResponseEntity<byte[]> export(
            @PathVariable String type,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ReportType reportType = parseType(type);
        DateRange range = DateRange.resolve(from, to);
        byte[] data = reportService.exportCsv(reportType, departmentId, range.from(), range.to(), userDetails.getUser());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + ReportService.csvFileName(reportType, range.from(), range.to()) + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(data);
    }

    private ReportType parseType(String value) {
        try {
            return ReportType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidRequestException("Invalid report type: " + value);
        }
    }
}
