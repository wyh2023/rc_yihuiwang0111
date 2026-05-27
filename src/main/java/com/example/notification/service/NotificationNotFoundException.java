package com.example.notification.service;

public class NotificationNotFoundException extends RuntimeException {
    public NotificationNotFoundException(String id) {
        super("Notification not found: " + id);
    }
}
