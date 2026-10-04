package com.govtrack360.dto.performance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Tells the UI which data the caller can see: an Officer is locked to one department, an Admin is not.
 */
@Getter
@Builder
@AllArgsConstructor
public class ScopeResponse {
    private boolean restricted;
    private Long departmentId;
    private String departmentName;
}
