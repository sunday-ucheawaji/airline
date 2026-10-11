package com.sunday.service.repository;

import java.util.UUID;
import com.sunday.service.model.Notification;
import com.sunday.service.model.NotificationChannel;
import com.sunday.service.model.NotificationEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Optional<Notification> findByEventTypeAndReferenceAndChannel(
            NotificationEventType eventType, String reference, NotificationChannel channel);

    List<Notification> findByReferenceOrderByCreatedAtDesc(String reference);
}
