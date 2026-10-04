package com.govtrack360.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ReportColumn {
    private String key;
    private String label;
    /** "text" or "number" (numbers are right-aligned in the UI). */
    private String type;
}
