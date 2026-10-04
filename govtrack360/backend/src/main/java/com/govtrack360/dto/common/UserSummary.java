package com.govtrack360.dto.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class UserSummary {
    private Long id;
    private String email;
    private String role;
    private String fullName;
}
