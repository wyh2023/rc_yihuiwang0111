package com.example.notification;

import java.nio.file.Path;

public class Main {
    public static void main(String[] args) throws Exception {
        int port = readIntEnv("PORT", 8080);
        Path dataFile = Path.of(System.getenv().getOrDefault("DATA_FILE", "data/notifications.json"));

        TaskRepository repository = new TaskRepository(dataFile);
        DeliveryClient deliveryClient = new DeliveryClient();
        DeliveryWorker worker = new DeliveryWorker(repository, deliveryClient);
        NotificationServer server = new NotificationServer(port, repository);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            worker.stop();
            server.stop();
        }));

        worker.start();
        server.start();
        System.out.println("Notification service started at http://localhost:" + port);
        System.out.println("Data file: " + dataFile.toAbsolutePath());
    }

    private static int readIntEnv(String key, int defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Integer.parseInt(value);
    }
}
