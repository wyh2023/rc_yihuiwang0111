package com.example.notification.worker;

import com.example.notification.domain.NotificationTask;
import com.example.notification.service.DeliveryResult;
import com.example.notification.service.DeliveryService;
import com.example.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NotificationDeliveryWorker {
    private static final Logger log = LoggerFactory.getLogger(NotificationDeliveryWorker.class);

    private final NotificationService notificationService;
    private final DeliveryService deliveryService;

    public NotificationDeliveryWorker(NotificationService notificationService, DeliveryService deliveryService) {
        this.notificationService = notificationService;
        this.deliveryService = deliveryService;
    }

    @Scheduled(fixedDelayString = "${notification.worker.fixed-delay-ms:2000}")
    public void deliverDueNotifications() {
        List<NotificationTask> tasks = notificationService.claimDueTasks();
        for (NotificationTask task : tasks) {
            DeliveryResult result = deliveryService.deliver(task);
            if (result.success()) {
                notificationService.markSuccess(task.getId());
                log.info("Delivered notification {}: {}", task.getId(), result.message());
            } else {
                notificationService.markFailure(task.getId(), result);
                log.warn("Delivery failed for notification {}: {}", task.getId(), result.message());
            }
        }
    }
}
