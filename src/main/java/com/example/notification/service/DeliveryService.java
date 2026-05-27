package com.example.notification.service;

import com.example.notification.domain.NotificationTask;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class DeliveryService {
    private static final TypeReference<Map<String, String>> HEADERS_TYPE = new TypeReference<>() {
    };

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public DeliveryService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public DeliveryResult deliver(NotificationTask task) {
        try {
            HttpHeaders headers = new HttpHeaders();
            Map<String, String> headerMap = objectMapper.readValue(task.getHeadersJson(), HEADERS_TYPE);
            headerMap.forEach(headers::set);

            HttpEntity<String> entity = new HttpEntity<>(task.getBodyJson(), headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    task.getTargetUrl(),
                    HttpMethod.valueOf(task.getMethod()),
                    entity,
                    String.class);

            int code = response.getStatusCode().value();
            if (code >= 200 && code < 300) {
                return new DeliveryResult(true, false, "HTTP " + code);
            }
            return new DeliveryResult(false, isRetryableStatus(code), "HTTP " + code);
        } catch (RestClientResponseException ex) {
            int code = ex.getStatusCode().value();
            return new DeliveryResult(false, isRetryableStatus(code), "HTTP " + code);
        } catch (ResourceAccessException ex) {
            return new DeliveryResult(false, true, ex.getClass().getSimpleName() + ": " + ex.getMessage());
        } catch (Exception ex) {
            return new DeliveryResult(false, false, ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    private boolean isRetryableStatus(int code) {
        return code == 408 || code == 429 || code >= 500;
    }
}
