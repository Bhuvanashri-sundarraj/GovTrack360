package com.govtrack360.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Every report is returned in the same tabular shape so one UI component can render all of them and
 * one CSV writer can export all of them.
 */
@Getter
@Builder
@AllArgsConstructor
public class ReportResponse {
    private String type;
    private String title;
    private String description;
    private LocalDate from;
    private LocalDate to;
    private String scope;
    private String generatedBy;
    private LocalDateTime generatedAt;
    private List<ReportSummaryItem> summary;
    private List<ReportColumn> columns;
    private List<Map<String, Object>> rows;
}
