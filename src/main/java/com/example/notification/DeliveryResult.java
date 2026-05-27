package com.example.notification;

public record DeliveryResult(boolean success, boolean retryable, String message) {
}
