package com.innovatech.notif.repository;

import com.innovatech.notif.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByTargetIdAndTargetTypeOrderByCreatedAtDesc(Long targetId, String targetType);
    List<Notification> findByTargetIdAndTargetTypeAndReadFalse(Long targetId, String targetType); // FIX: "IsRead" no resuelve, el campo/getter Lombok es "read"
}