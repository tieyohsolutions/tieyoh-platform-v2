package com.tieyoh.api.auth.service;

import com.tieyoh.api.auth.dto.NotificationResult;
import com.tieyoh.api.auth.entity.Notification;
import com.tieyoh.api.auth.repository.NotificationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

// Backs the bell icon. "type" is deliberately just a string (see the
// Notification entity) so future kinds -- a gift received, once the
// gift-sending flow itself exists -- can be added without touching this
// service's shape, just a new call to notify(...) from wherever that event
// happens.
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public void notify(Long userId, String type, Long actorUserId, String actorDisplayName,
                        String message, Long connectionRequestId, Long giftId) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setActorUserId(actorUserId);
        n.setActorDisplayName(actorDisplayName);
        n.setMessage(message);
        n.setConnectionRequestId(connectionRequestId);
        n.setGiftId(giftId);
        notificationRepository.save(n);
    }

    public List<NotificationResult> getNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(n -> new NotificationResult(
                        n.getId(),
                        n.getType(),
                        n.getActorUserId(),
                        n.getActorDisplayName(),
                        n.getMessage(),
                        n.isRead(),
                        n.getCreatedAt() == null ? null : n.getCreatedAt().toString(),
                        n.getConnectionRequestId(),
                        n.getGiftId()))
                .collect(Collectors.toList());
    }

    public void markAllRead(Long userId) {
        List<Notification> unread = notificationRepository.findByUserIdAndReadFalse(userId);
        for (Notification n : unread) {
            n.setRead(true);
        }
        notificationRepository.saveAll(unread);
    }

    // Marks a single notification read -- what the bell screen calls now
    // when the user actually taps a row, rather than blanket-marking
    // everything read the instant the screen opens (which never gave the
    // unread/read distinction a chance to mean anything visually).
    public void markRead(Long userId, Long notificationId) {
        Notification n = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        if (!n.isRead()) {
            n.setRead(true);
            notificationRepository.save(n);
        }
    }
}
