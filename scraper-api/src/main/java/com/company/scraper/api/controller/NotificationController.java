package com.company.scraper.api.controller;

import com.company.scraper.common.dto.NotificationRequest;
import com.company.scraper.common.model.Notification;
import com.company.scraper.common.notification.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    public ResponseEntity<Notification> send(@Valid @RequestBody NotificationRequest request) {
        return ResponseEntity.ok(notificationService.send(request));
    }
}
