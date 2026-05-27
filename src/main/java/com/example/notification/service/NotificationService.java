package com.example.notification.service;

import com.example.notification.api.CreateNotificationRequest;
import com.example.notification.config.DeliveryProperties;
import com.example.notification.domain.NotificationStatus;
import com.example.notification.domain.NotificationTask;
import com.example.notification.repository.NotificationTaskRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class NotificationService {
    private final NotificationTaskRepository repository;
    private final ObjectMapper objectMapper;
    private final DeliveryProperties properties;

    public NotificationService(
            NotificationTaskRepository repository,
            ObjectMapper objectMapper,
            DeliveryProperties properties) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Transactional
    public NotificationTask create(CreateNotificationRequest request) {
        validateUrl(request.getTargetUrl());
        String method = normalizeMethod(request.getMethod());
        Instant now = Instant.now();

        NotificationTask task = new NotificationTask(
                "ntf_" + UUID.randomUUID().toString().replace("-", ""),
                request.getTargetUrl(),
                method,
                writeJson(request.getHeaders()),
                request.getBody() == null ? "{}" : writeJson(request.getBody()),
                request.getMaxAttempts(),
                now);
        return repository.save(task);
    }

    public NotificationTask get(String id) {
        return repository.findById(id).orElseThrow(() -> new NotificationNotFoundException(id));
    }

    @Transactional
    public NotificationTask retry(String id) {
        NotificationTask task = get(id);
        if (task.getStatus() != NotificationStatus.FAILED) {
            throw new IllegalArgumentException("Only failed notifications can be retried");
        }
        task.markRetryRequested(Instant.now());
        return task;
    }

    @Transactional
    public List<NotificationTask> claimDueTasks() {
        Instant now = Instant.now();
        recoverStaleProcessingTasks(now);
        List<NotificationTask> tasks = repository.findDueTasksForUpdate(
                NotificationStatus.PENDING,
                now,
                PageRequest.of(0, properties.getBatchSize()));
        for (NotificationTask task : tasks) {
            task.markProcessing(now);
        }
        return tasks;
    }

    private void recoverStaleProcessingTasks(Instant now) {
        Instant deadline = now.minus(properties.getProcessingTimeout());
        List<NotificationTask> staleTasks = repository.findStaleProcessingTasksForUpdate(
                NotificationStatus.PROCESSING,
                deadline);
        for (NotificationTask task : staleTasks) {
            task.markRecovered(now);
        }
    }

    @Transactional
    public void markSuccess(String id) {
        get(id).markSuccess(Instant.now());
    }

    @Transactional
    public void markFailure(String id, DeliveryResult result) {
        NotificationTask task = get(id);
        Instant now = Instant.now();
        task.markFailure(result.message(), result.retryable(), now, now.plus(backoff(task.getAttemptCount() + 1)));
    }

    private Duration backoff(int nextAttempt) {
        return switch (nextAttempt) {
            case 1 -> Duration.ofMinutes(1);
            case 2 -> Duration.ofMinutes(5);
            case 3 -> Duration.ofMinutes(15);
            default -> Duration.ofHours(1);
        };
    }

    private void validateUrl(String targetUrl) {
        URI uri = URI.create(targetUrl);
        if (uri.getScheme() == null
                || uri.getHost() == null
                || !(uri.getScheme().equalsIgnoreCase("http") || uri.getScheme().equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException("targetUrl must be an absolute http(s) URL");
        }
    }

    private String normalizeMethod(String method) {
        String normalized = method == null || method.isBlank()
                ? "POST"
                : method.toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "GET", "POST", "PUT", "PATCH", "DELETE" -> normalized;
            default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        };
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Invalid JSON payload", ex);
        }
    }
}
