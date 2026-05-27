package com.example.notification.worker;

import com.example.notification.domain.NotificationTask;
import com.example.notification.service.DeliveryResult;
import com.example.notification.service.DeliveryService;
import com.example.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Component
public class NotificationDeliveryWorker {
    private static final Logger log = LoggerFactory.getLogger(NotificationDeliveryWorker.class);

    private final NotificationService notificationService;
    private final DeliveryService deliveryService;
    private final ThreadPoolTaskExecutor deliveryExecutor;

    public NotificationDeliveryWorker(
            NotificationService notificationService,
            DeliveryService deliveryService,
            @Qualifier("notificationDeliveryExecutor") ThreadPoolTaskExecutor deliveryExecutor) {
        this.notificationService = notificationService;
        this.deliveryService = deliveryService;
        this.deliveryExecutor = deliveryExecutor;
    }

    @Scheduled(fixedDelayString = "${notification.worker.fixed-delay-ms:2000}")
    public void deliverDueNotifications() {
        List<NotificationTask> tasks = notificationService.claimDueTasks();
        if (tasks.isEmpty()) {
            return;
        }

        List<Future<?>> deliveries = new ArrayList<>(tasks.size());
        for (NotificationTask task : tasks) {
            deliveries.add(deliveryExecutor.submit(() -> doRequest(task)));
        }

        waitForDeliveries(deliveries);
    }

    private void waitForDeliveries(List<Future<?>> deliveries) {
        for (Future<?> delivery : deliveries) {
            try {
                delivery.get();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                log.warn("Notification delivery worker was interrupted while waiting for delivery tasks");
                return;
            } catch (ExecutionException ex) {
                log.error("Notification delivery task failed unexpectedly", ex.getCause());
            }
        }
    }

    private void doRequest(NotificationTask task) {
        try {
            DeliveryResult result = deliveryService.deliver(task);
            if (result.success()) {
                notificationService.markSuccess(task.getId());
                log.info("Delivered notification {}: {}", task.getId(), result.message());
                return;
            }

            notificationService.markFailure(task.getId(), result);
            log.warn("Delivery failed for notification {}: {}", task.getId(), result.message());
        } catch (RuntimeException ex) {
            log.error("Unexpected delivery error for notification {}", task.getId(), ex);
        }
    }
}
