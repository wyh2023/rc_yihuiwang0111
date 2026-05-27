package com.example.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

@Entity
@Table(name = "notification_tasks")
public class NotificationTask {
    @Id
    @Column(length = 40)
    private String id;

    @Column(name = "target_url", nullable = false, length = 2048)
    private String targetUrl;

    @Column(nullable = false, length = 16)
    private String method;

    @Lob
    @Column(name = "headers_json", nullable = false, columnDefinition = "TEXT")
    private String headersJson;

    @Lob
    @Column(name = "body_json", nullable = false, columnDefinition = "TEXT")
    private String bodyJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NotificationStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Column(name = "next_retry_at", nullable = false)
    private Instant nextRetryAt;

    @Column(name = "last_error", length = 2000)
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected NotificationTask() {
    }

    public NotificationTask(
            String id,
            String targetUrl,
            String method,
            String headersJson,
            String bodyJson,
            int maxAttempts,
            Instant now) {
        this.id = id;
        this.targetUrl = targetUrl;
        this.method = method;
        this.headersJson = headersJson;
        this.bodyJson = bodyJson;
        this.status = NotificationStatus.PENDING;
        this.attemptCount = 0;
        this.maxAttempts = maxAttempts;
        this.nextRetryAt = now;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public String getId() {
        return id;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public String getMethod() {
        return method;
    }

    public String getHeadersJson() {
        return headersJson;
    }

    public String getBodyJson() {
        return bodyJson;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public Instant getNextRetryAt() {
        return nextRetryAt;
    }

    public String getLastError() {
        return lastError;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void markProcessing(Instant now) {
        this.status = NotificationStatus.PROCESSING;
        this.updatedAt = now;
    }

    public void markSuccess(Instant now) {
        this.status = NotificationStatus.SUCCESS;
        this.lastError = null;
        this.updatedAt = now;
    }

    public void markFailure(String error, boolean retryable, Instant now, Instant nextRetryAt) {
        this.attemptCount++;
        this.lastError = error;
        this.updatedAt = now;

        if (retryable && this.attemptCount < this.maxAttempts) {
            this.status = NotificationStatus.PENDING;
            this.nextRetryAt = nextRetryAt;
        } else {
            this.status = NotificationStatus.FAILED;
        }
    }

    public void markRetryRequested(Instant now) {
        this.status = NotificationStatus.PENDING;
        this.nextRetryAt = now;
        this.lastError = null;
        this.updatedAt = now;
    }

    public void markRecovered(Instant now) {
        this.status = NotificationStatus.PENDING;
        this.nextRetryAt = now;
        this.lastError = "Recovered stale processing task";
        this.updatedAt = now;
    }
}
