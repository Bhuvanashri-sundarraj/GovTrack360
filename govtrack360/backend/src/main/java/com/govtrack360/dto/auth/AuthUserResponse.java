package com.govtrack360.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AuthUserResponse {
    private Long id;
    private String email;
    private String role;
    private boolean enabled;
    private Long employeeId;
    private String employeeCode;
    private String fullName;
}
