package com.tieyoh.api.auth.repository;

import com.tieyoh.api.auth.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Notification> findByUserIdAndReadFalse(Long userId);

    // Scoped by userId too, not just id -- so one user can never mark
    // another user's notification read by guessing an id (the same
    // trust-model caveat as the rest of this backend: there's no real
    // auth session, but this keeps the endpoint from being a completely
    // open door regardless).
    java.util.Optional<Notification> findByIdAndUserId(Long id, Long userId);
}
