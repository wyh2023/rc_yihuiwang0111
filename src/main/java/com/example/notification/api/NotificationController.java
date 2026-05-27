package com.example.notification.api;

import com.example.notification.domain.NotificationTask;
import com.example.notification.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @PostMapping("/notifications")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CreateNotificationResponse create(@Valid @RequestBody CreateNotificationRequest request) {
        NotificationTask task = notificationService.create(request);
        return new CreateNotificationResponse(task.getId(), task.getStatus().name().toLowerCase());
    }

    @GetMapping("/notifications/{id}")
    public NotificationResponse get(@PathVariable String id) {
        return NotificationResponse.from(notificationService.get(id));
    }

    @PostMapping("/notifications/{id}/retry")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CreateNotificationResponse retry(@PathVariable String id) {
        NotificationTask task = notificationService.retry(id);
        return new CreateNotificationResponse(task.getId(), task.getStatus().name().toLowerCase());
    }
}
