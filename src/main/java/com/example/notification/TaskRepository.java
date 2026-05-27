package com.example.notification;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TaskRepository {
    private static final Duration PROCESSING_TIMEOUT = Duration.ofMinutes(5);

    private final Path dataFile;
    private final List<NotificationTask> tasks = new ArrayList<>();

    public TaskRepository(Path dataFile) throws IOException {
        this.dataFile = dataFile;
        load();
    }

    public synchronized NotificationTask add(NotificationTask task) throws IOException {
        tasks.add(task);
        save();
        return task;
    }

    public synchronized Optional<NotificationTask> findById(String id) {
        return tasks.stream().filter(task -> task.id.equals(id)).findFirst();
    }

    public synchronized Optional<NotificationTask> claimDueTask() throws IOException {
        Instant now = Instant.now();
        recoverStaleProcessingTasks(now);

        Optional<NotificationTask> task = tasks.stream()
                .filter(item -> item.status == TaskStatus.PENDING)
                .filter(item -> !item.nextRetryAt.isAfter(now))
                .min(Comparator.comparing(item -> item.nextRetryAt));

        if (task.isPresent()) {
            NotificationTask value = task.get();
            value.status = TaskStatus.PROCESSING;
            value.updatedAt = now;
            save();
        }
        return task;
    }

    public synchronized void markSuccess(String id) throws IOException {
        NotificationTask task = require(id);
        task.status = TaskStatus.SUCCESS;
        task.lastError = null;
        task.updatedAt = Instant.now();
        save();
    }

    public synchronized void markFailure(String id, String error, boolean retryable) throws IOException {
        NotificationTask task = require(id);
        task.attemptCount++;
        task.lastError = error;
        task.updatedAt = Instant.now();

        if (retryable && task.attemptCount < task.maxAttempts) {
            task.status = TaskStatus.PENDING;
            task.nextRetryAt = task.updatedAt.plus(backoff(task.attemptCount));
        } else {
            task.status = TaskStatus.FAILED;
        }
        save();
    }

    public synchronized NotificationTask retry(String id) throws IOException {
        NotificationTask task = require(id);
        if (task.status != TaskStatus.FAILED) {
            throw new IllegalArgumentException("Only failed tasks can be retried");
        }
        task.status = TaskStatus.PENDING;
        task.nextRetryAt = Instant.now();
        task.lastError = null;
        task.updatedAt = task.nextRetryAt;
        save();
        return task;
    }

    private NotificationTask require(String id) {
        return findById(id).orElseThrow(() -> new IllegalArgumentException("Task not found: " + id));
    }

    private void recoverStaleProcessingTasks(Instant now) {
        for (NotificationTask task : tasks) {
            if (task.status == TaskStatus.PROCESSING
                    && task.updatedAt.plus(PROCESSING_TIMEOUT).isBefore(now)) {
                task.status = TaskStatus.PENDING;
                task.nextRetryAt = now;
                task.lastError = "Recovered stale processing task";
                task.updatedAt = now;
            }
        }
    }

    private Duration backoff(int attemptCount) {
        return switch (attemptCount) {
            case 1 -> Duration.ofMinutes(1);
            case 2 -> Duration.ofMinutes(5);
            case 3 -> Duration.ofMinutes(15);
            default -> Duration.ofHours(1);
        };
    }

    @SuppressWarnings("unchecked")
    private void load() throws IOException {
        if (!Files.exists(dataFile)) {
            return;
        }
        String content = Files.readString(dataFile, StandardCharsets.UTF_8).trim();
        if (content.isEmpty()) {
            return;
        }
        Object parsed = Json.parse(content);
        if (!(parsed instanceof List<?> list)) {
            throw new IOException("Data file must contain a JSON array");
        }
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                tasks.add(NotificationTask.fromMap((Map<String, Object>) map));
            }
        }
    }

    private void save() throws IOException {
        if (dataFile.getParent() != null) {
            Files.createDirectories(dataFile.getParent());
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (NotificationTask task : tasks) {
            rows.add(new LinkedHashMap<>(task.toMap()));
        }
        Files.writeString(dataFile, Json.stringify(rows), StandardCharsets.UTF_8);
    }
}
