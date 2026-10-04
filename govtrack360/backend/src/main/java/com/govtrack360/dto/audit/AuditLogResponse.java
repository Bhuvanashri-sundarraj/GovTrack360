package com.govtrack360.dto.audit;

import com.govtrack360.dto.common.UserSummary;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class AuditLogResponse {
    private Long id;
    private UserSummary user;
    private String role;
    private String action;
    private String module;
    private String description;
    private LocalDateTime timestamp;
}
