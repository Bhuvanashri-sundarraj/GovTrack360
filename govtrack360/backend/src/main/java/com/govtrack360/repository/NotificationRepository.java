package com.govtrack360.repository;

import com.govtrack360.entity.Notification;
import com.govtrack360.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientOrderByCreatedAtDesc(User recipient);
    long countByRecipientAndIsReadFalse(User recipient);
    List<Notification> findByRecipientAndIsReadFalse(User recipient);
}
