package com.example.notification;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;

public class DeliveryClient {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .build();

    public DeliveryResult deliver(NotificationTask task) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(task.targetUrl))
                    .timeout(REQUEST_TIMEOUT);

            task.headers.forEach(builder::header);
            if (!task.headers.keySet().stream().map(String::toLowerCase).toList().contains("content-type")
                    && task.body != null
                    && !task.body.isBlank()) {
                builder.header("Content-Type", "application/json");
            }

            String method = task.method.toUpperCase(Locale.ROOT);
            if (method.equals("GET") || method.equals("DELETE")) {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            } else {
                builder.method(method, HttpRequest.BodyPublishers.ofString(task.body == null ? "" : task.body));
            }

            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            int code = response.statusCode();

            if (code >= 200 && code < 300) {
                return new DeliveryResult(true, false, "HTTP " + code);
            }
            if (code == 408 || code == 429 || code >= 500) {
                return new DeliveryResult(false, true, "Retryable HTTP " + code);
            }
            return new DeliveryResult(false, false, "Non-retryable HTTP " + code);
        } catch (Exception ex) {
            return new DeliveryResult(false, true, ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }
}
