package com.govtrack360.dto.profile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor
public class ProfileResponse {
    private Long userId;
    private String email;
    private String role;
    private boolean accountEnabled;

    // Employee-linked fields (null for pure admin accounts with no Employee record)
    private Long employeeRecordId;
    private String employeeId;
    private String firstName;
    private String lastName;
    private String phone;
    private String designation;
    private String departmentName;
    private LocalDate joiningDate;
    private String employmentStatus;
}
