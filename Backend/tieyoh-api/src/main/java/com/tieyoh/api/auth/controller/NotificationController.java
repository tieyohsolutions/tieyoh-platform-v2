package com.tieyoh.api.auth.controller;

import com.tieyoh.api.auth.dto.NotificationResult;
import com.tieyoh.api.auth.service.NotificationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationResult> getNotifications(@PathVariable Long userId) {
        return notificationService.getNotifications(userId);
    }

    @PostMapping("/mark-read")
    public void markAllRead(@PathVariable Long userId) {
        notificationService.markAllRead(userId);
    }

    @PostMapping("/{notificationId}/mark-read")
    public void markRead(@PathVariable Long userId, @PathVariable Long notificationId) {
        notificationService.markRead(userId, notificationId);
    }
}
