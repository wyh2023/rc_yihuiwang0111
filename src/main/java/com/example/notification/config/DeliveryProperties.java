package com.example.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "notification.delivery")
public class DeliveryProperties {
    private Duration connectTimeout = Duration.ofSeconds(5);
    private Duration readTimeout = Duration.ofSeconds(10);
    private Duration processingTimeout = Duration.ofMinutes(5);
    private int batchSize = 10;
    private int poolCoreSize = 4;
    private int poolMaxSize = 8;
    private int poolQueueCapacity = 100;

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

    public Duration getProcessingTimeout() {
        return processingTimeout;
    }

    public void setProcessingTimeout(Duration processingTimeout) {
        this.processingTimeout = processingTimeout;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getPoolCoreSize() {
        return poolCoreSize;
    }

    public void setPoolCoreSize(int poolCoreSize) {
        this.poolCoreSize = poolCoreSize;
    }

    public int getPoolMaxSize() {
        return poolMaxSize;
    }

    public void setPoolMaxSize(int poolMaxSize) {
        this.poolMaxSize = poolMaxSize;
    }

    public int getPoolQueueCapacity() {
        return poolQueueCapacity;
    }

    public void setPoolQueueCapacity(int poolQueueCapacity) {
        this.poolQueueCapacity = poolQueueCapacity;
    }
}
