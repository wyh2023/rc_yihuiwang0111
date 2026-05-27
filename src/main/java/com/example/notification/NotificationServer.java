package com.example.notification;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

public class NotificationServer {
    private final HttpServer server;
    private final TaskRepository repository;

    public NotificationServer(int port, TaskRepository repository) throws IOException {
        this.repository = repository;
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.server.createContext("/", this::handle);
        this.server.setExecutor(Executors.newFixedThreadPool(8));
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(1);
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
            String path = exchange.getRequestURI().getPath();

            if (method.equals("GET") && path.equals("/health")) {
                writeJson(exchange, 200, Map.of("status", "ok"));
                return;
            }

            if (method.equals("POST") && path.equals("/notifications")) {
                createNotification(exchange);
                return;
            }

            if (method.equals("GET") && path.startsWith("/notifications/")) {
                getNotification(exchange, path.substring("/notifications/".length()));
                return;
            }

            if (method.equals("POST") && path.startsWith("/notifications/") && path.endsWith("/retry")) {
                String id = path.substring("/notifications/".length(), path.length() - "/retry".length());
                retryNotification(exchange, id);
                return;
            }

            writeJson(exchange, 404, Map.of("error", "Not found"));
        } catch (IllegalArgumentException ex) {
            writeJson(exchange, 400, errorBody(ex.getMessage()));
        } catch (Exception ex) {
            writeJson(exchange, 500, Map.of("error", "Internal server error"));
            ex.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private void createNotification(HttpExchange exchange) throws IOException {
        String requestBody = readBody(exchange);
        Object parsed = Json.parse(requestBody);
        if (!(parsed instanceof Map<?, ?> rawMap)) {
            throw new IllegalArgumentException("Request body must be a JSON object");
        }
        Map<String, Object> map = (Map<String, Object>) rawMap;
        String targetUrl = requiredString(map, "target_url");
        validateUrl(targetUrl);

        String method = stringValue(map.getOrDefault("method", "POST")).toUpperCase(Locale.ROOT);
        int maxAttempts = intValue(map.getOrDefault("max_attempts", 5));
        if (maxAttempts < 1 || maxAttempts > 20) {
            throw new IllegalArgumentException("max_attempts must be between 1 and 20");
        }

        Map<String, String> headers = new LinkedHashMap<>();
        Object rawHeaders = map.get("headers");
        if (rawHeaders instanceof Map<?, ?> headerMap) {
            for (Map.Entry<?, ?> entry : headerMap.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    headers.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
                }
            }
        }

        Object rawPayload = map.get("body");
        String payload = rawPayload == null ? "" : Json.stringify(rawPayload);
        NotificationTask task = NotificationTask.create(targetUrl, method, headers, payload, maxAttempts);
        repository.add(task);

        writeJson(exchange, 202, Map.of(
                "notification_id", task.id,
                "status", task.status.name().toLowerCase()));
    }

    private void getNotification(HttpExchange exchange, String id) throws IOException {
        repository.findById(id)
                .ifPresentOrElse(
                        task -> writeJsonUnchecked(exchange, 200, task.toMap()),
                        () -> writeJsonUnchecked(exchange, 404, Map.of("error", "Notification not found")));
    }

    private void retryNotification(HttpExchange exchange, String id) throws IOException {
        NotificationTask task = repository.retry(id);
        writeJson(exchange, 202, Map.of(
                "notification_id", task.id,
                "status", task.status.name().toLowerCase()));
    }

    private void validateUrl(String targetUrl) {
        URI uri = URI.create(targetUrl);
        if (uri.getScheme() == null
                || !(uri.getScheme().equalsIgnoreCase("http") || uri.getScheme().equalsIgnoreCase("https"))
                || uri.getHost() == null) {
            throw new IllegalArgumentException("target_url must be an absolute http(s) URL");
        }
    }

    private String readBody(HttpExchange exchange) throws IOException {
        try (InputStream input = exchange.getRequestBody()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void writeJsonUnchecked(HttpExchange exchange, int statusCode, Object body) {
        try {
            writeJson(exchange, statusCode, body);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void writeJson(HttpExchange exchange, int statusCode, Object body) throws IOException {
        byte[] bytes = Json.stringify(body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private Map<String, String> errorBody(String message) {
        return Map.of("error", message == null ? "Bad request" : message);
    }

    private String requiredString(Map<String, Object> map, String key) {
        String value = stringValue(map.get(key));
        if (value.isBlank()) {
            throw new IllegalArgumentException(key + " is required");
        }
        return value;
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private int intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }
}
