package com.example.notification.api;

import com.example.notification.domain.NotificationTask;

import java.time.Instant;

public record NotificationResponse(
        String id,
        String targetUrl,
        String method,
        String status,
        int attemptCount,
        int maxAttempts,
        Instant nextRetryAt,
        String lastError,
        Instant createdAt,
        Instant updatedAt) {
    public static NotificationResponse from(NotificationTask task) {
        return new NotificationResponse(
                task.getId(),
                task.getTargetUrl(),
                task.getMethod(),
                task.getStatus().name().toLowerCase(),
                task.getAttemptCount(),
                task.getMaxAttempts(),
                task.getNextRetryAt(),
                task.getLastError(),
                task.getCreatedAt(),
                task.getUpdatedAt());
    }
}
