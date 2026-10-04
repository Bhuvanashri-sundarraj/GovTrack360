package com.govtrack360.controller;

import com.govtrack360.dto.notification.NotificationResponse;
import com.govtrack360.dto.notification.UnreadCountResponse;
import com.govtrack360.security.CustomUserDetails;
import com.govtrack360.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER','EMPLOYEE')")
    public ResponseEntity<List<NotificationResponse>> getMy(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(notificationService.getMy(userDetails.getUser()));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER','EMPLOYEE')")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(@AuthenticationPrincipal CustomUserDetails userDetails) {
        long count = notificationService.getUnreadCount(userDetails.getUser());
        return ResponseEntity.ok(UnreadCountResponse.builder().unreadCount(count).build());
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER','EMPLOYEE')")
    public ResponseEntity<NotificationResponse> markRead(@PathVariable Long id,
                                                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(notificationService.markRead(id, userDetails.getUser()));
    }

    @PatchMapping("/read-all")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER','EMPLOYEE')")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal CustomUserDetails userDetails) {
        notificationService.markAllRead(userDetails.getUser());
        return ResponseEntity.noContent().build();
    }
}
