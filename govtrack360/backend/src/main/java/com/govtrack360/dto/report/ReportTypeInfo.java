package com.govtrack360.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ReportTypeInfo {
    private String value;
    private String label;
    private String description;
}
