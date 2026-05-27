package com.example.notification;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class NotificationTask {
    public final String id;
    public final String targetUrl;
    public final String method;
    public final Map<String, String> headers;
    public final String body;
    public TaskStatus status;
    public int attemptCount;
    public final int maxAttempts;
    public Instant nextRetryAt;
    public String lastError;
    public Instant createdAt;
    public Instant updatedAt;

    public NotificationTask(
            String id,
            String targetUrl,
            String method,
            Map<String, String> headers,
            String body,
            TaskStatus status,
            int attemptCount,
            int maxAttempts,
            Instant nextRetryAt,
            String lastError,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.targetUrl = targetUrl;
        this.method = method;
        this.headers = new LinkedHashMap<>(headers);
        this.body = body;
        this.status = status;
        this.attemptCount = attemptCount;
        this.maxAttempts = maxAttempts;
        this.nextRetryAt = nextRetryAt;
        this.lastError = lastError;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static NotificationTask create(
            String targetUrl,
            String method,
            Map<String, String> headers,
            String body,
            int maxAttempts) {
        Instant now = Instant.now();
        return new NotificationTask(
                "ntf_" + UUID.randomUUID().toString().replace("-", ""),
                targetUrl,
                method,
                headers,
                body,
                TaskStatus.PENDING,
                0,
                maxAttempts,
                now,
                null,
                now,
                now);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("target_url", targetUrl);
        map.put("method", method);
        map.put("headers", headers);
        map.put("body", body);
        map.put("status", status.name().toLowerCase());
        map.put("attempt_count", attemptCount);
        map.put("max_attempts", maxAttempts);
        map.put("next_retry_at", nextRetryAt.toString());
        map.put("last_error", lastError);
        map.put("created_at", createdAt.toString());
        map.put("updated_at", updatedAt.toString());
        return map;
    }

    @SuppressWarnings("unchecked")
    public static NotificationTask fromMap(Map<String, Object> map) {
        Map<String, String> headers = new LinkedHashMap<>();
        Object rawHeaders = map.get("headers");
        if (rawHeaders instanceof Map<?, ?> headerMap) {
            for (Map.Entry<?, ?> entry : headerMap.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    headers.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
                }
            }
        }

        return new NotificationTask(
                stringValue(map.get("id")),
                stringValue(map.get("target_url")),
                stringValue(map.getOrDefault("method", "POST")),
                headers,
                stringValue(map.getOrDefault("body", "")),
                TaskStatus.valueOf(stringValue(map.get("status")).toUpperCase()),
                intValue(map.get("attempt_count"), 0),
                intValue(map.get("max_attempts"), 5),
                Instant.parse(stringValue(map.get("next_retry_at"))),
                nullableString(map.get("last_error")),
                Instant.parse(stringValue(map.get("created_at"))),
                Instant.parse(stringValue(map.get("updated_at"))));
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String nullableString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static int intValue(Object value, int defaultValue) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return defaultValue;
        }
        return Integer.parseInt(String.valueOf(value));
    }
}
