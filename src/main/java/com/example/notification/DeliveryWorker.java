package com.example.notification;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class DeliveryWorker {
    private final TaskRepository repository;
    private final DeliveryClient deliveryClient;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread thread;

    public DeliveryWorker(TaskRepository repository, DeliveryClient deliveryClient) {
        this.repository = repository;
        this.deliveryClient = deliveryClient;
    }

    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        thread = new Thread(this::runLoop, "delivery-worker");
        thread.setDaemon(false);
        thread.start();
    }

    public void stop() {
        running.set(false);
        if (thread != null) {
            thread.interrupt();
        }
    }

    private void runLoop() {
        while (running.get()) {
            try {
                Optional<NotificationTask> task = repository.claimDueTask();
                if (task.isEmpty()) {
                    sleep(2000);
                    continue;
                }
                deliver(task.get());
            } catch (Exception ex) {
                System.err.println("Worker error: " + ex.getMessage());
                sleep(2000);
            }
        }
    }

    private void deliver(NotificationTask task) throws IOException {
        DeliveryResult result = deliveryClient.deliver(task);
        if (result.success()) {
            repository.markSuccess(task.id);
            System.out.println("Delivered " + task.id + ": " + result.message());
        } else {
            repository.markFailure(task.id, result.message(), result.retryable());
            System.out.println("Delivery failed " + task.id + ": " + result.message());
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
