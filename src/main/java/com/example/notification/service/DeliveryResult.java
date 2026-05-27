package com.example.notification.service;

public record DeliveryResult(boolean success, boolean retryable, String message) {
}
