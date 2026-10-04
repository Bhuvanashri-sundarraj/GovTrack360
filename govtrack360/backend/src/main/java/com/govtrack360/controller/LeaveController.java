package com.govtrack360.controller;

import com.govtrack360.dto.leave.LeaveRequestCreateRequest;
import com.govtrack360.dto.leave.LeaveRequestResponse;
import com.govtrack360.dto.leave.LeaveReviewRequest;
import com.govtrack360.entity.LeaveStatus;
import com.govtrack360.exception.InvalidRequestException;
import com.govtrack360.security.CustomUserDetails;
import com.govtrack360.service.LeaveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leave-requests")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER','EMPLOYEE')")
    public ResponseEntity<LeaveRequestResponse> apply(@Valid @RequestBody LeaveRequestCreateRequest request,
                                                        @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED).body(leaveService.apply(request, userDetails.getUser()));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER','EMPLOYEE')")
    public ResponseEntity<List<LeaveRequestResponse>> getMy(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(leaveService.getMy(userDetails.getUser()));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER','EMPLOYEE')")
    public ResponseEntity<LeaveRequestResponse> cancel(@PathVariable Long id,
                                                         @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(leaveService.cancel(id, userDetails.getUser()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER')")
    public ResponseEntity<List<LeaveRequestResponse>> search(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        LeaveStatus leaveStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                leaveStatus = LeaveStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new InvalidRequestException("Invalid status value: " + status);
            }
        }
        return ResponseEntity.ok(leaveService.search(employeeId, departmentId, leaveStatus, userDetails.getUser()));
    }

    @PatchMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER')")
    public ResponseEntity<LeaveRequestResponse> review(@PathVariable Long id, @Valid @RequestBody LeaveReviewRequest request,
                                                         @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(leaveService.review(id, request, userDetails.getUser()));
    }
}
