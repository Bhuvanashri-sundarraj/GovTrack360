package com.govtrack360.service;

import com.govtrack360.dto.notification.NotificationResponse;
import com.govtrack360.entity.Notification;
import com.govtrack360.entity.NotificationType;
import com.govtrack360.entity.User;
import com.govtrack360.exception.ResourceNotFoundException;
import com.govtrack360.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Notification API: creation is used internally by other services (Task, Attendance, Leave);
 * the read/mark-as-read methods below back the user-facing REST endpoints.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public void notify(User recipient, String title, String message, NotificationType type) {
        if (recipient == null) {
            return; // nothing to notify (e.g. unassigned task)
        }
        Notification notification = Notification.builder()
                .recipient(recipient)
                .title(title)
                .message(message)
                .type(type)
                .build();
        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getMy(User actor) {
        return notificationRepository.findByRecipientOrderByCreatedAtDesc(actor)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(User actor) {
        return notificationRepository.countByRecipientAndIsReadFalse(actor);
    }

    @Transactional
    public NotificationResponse markRead(Long id, User actor) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + id));

        if (!notification.getRecipient().getId().equals(actor.getId())) {
            throw new AccessDeniedException("You can only manage your own notifications.");
        }

        notification.setRead(true);
        notification = notificationRepository.save(notification);
        return toResponse(notification);
    }

    @Transactional
    public void markAllRead(User actor) {
        List<Notification> unread = notificationRepository.findByRecipientAndIsReadFalse(actor);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType().name())
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
