package com.govtrack360.controller;

import com.govtrack360.dto.reward.*;
import com.govtrack360.security.CustomUserDetails;
import com.govtrack360.service.RewardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/rewards")
@RequiredArgsConstructor
public class RewardController {

    private final RewardService rewardService;

    @GetMapping("/types")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER','EMPLOYEE')")
    public ResponseEntity<List<RewardTypeResponse>> getTypes() {
        return ResponseEntity.ok(rewardService.getTypes());
    }

    @GetMapping("/wall")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER','EMPLOYEE')")
    public ResponseEntity<List<RewardResponse>> getWall(@RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(rewardService.getWall(limit));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER','EMPLOYEE')")
    public ResponseEntity<MyRewardsResponse> getMine(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(rewardService.getMine(userDetails.getUser()));
    }

    @GetMapping("/leaderboard")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER')")
    public ResponseEntity<List<RewardLeaderboardEntry>> getLeaderboard(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(rewardService.getLeaderboard(userDetails.getUser(), departmentId, from, to));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER')")
    public ResponseEntity<List<RewardResponse>> search(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(rewardService.search(userDetails.getUser(), employeeId, departmentId, type, from, to));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER')")
    public ResponseEntity<RewardResponse> grant(@Valid @RequestBody RewardRequest request,
                                                 @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rewardService.grant(request, userDetails.getUser()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER')")
    public ResponseEntity<Void> revoke(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails userDetails) {
        rewardService.revoke(id, userDetails.getUser());
        return ResponseEntity.noContent().build();
    }
}
