package com.sprih.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprih.notification.dto.CallbackRequest;
import com.sprih.notification.model.Event;
import com.sprih.notification.model.EventStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Service responsible for sending callback notifications to client systems.
 * Handles HTTP POST requests with event status updates.
 */
@Service
public class CallbackService {
    
    private static final Logger logger = LoggerFactory.getLogger(CallbackService.class);
    
    @Value("${notification.callback.timeout:5000}")
    private int callbackTimeout;
    
    @Value("${notification.callback.retries:3}")
    private int maxRetries;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public CallbackService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules(); // Register all modules including JSR310
    }

    /**
     * Sends a callback notification for the given event.
     * 
     * @param event The completed or failed event
     */
    public void sendCallback(Event event) {
        if (event.getCallbackUrl() == null || event.getCallbackUrl().trim().isEmpty()) {
            logger.warn("No callback URL provided for event {}, skipping callback", event.getEventId());
            return;
        }

        CallbackRequest callbackRequest = createCallbackRequest(event);
        
        logger.info("Sending callback for event {} to URL: {}", event.getEventId(), event.getCallbackUrl());
        
        boolean success = false;
        int attempt = 0;
        
        while (!success && attempt < maxRetries) {
            attempt++;
            try {
                sendCallbackRequest(event.getCallbackUrl(), callbackRequest);
                success = true;
                logger.info("Callback sent successfully for event {} on attempt {}", event.getEventId(), attempt);
                
            } catch (Exception e) {
                logger.warn("Failed to send callback for event {} on attempt {}: {}", 
                           event.getEventId(), attempt, e.getMessage());
                
                if (attempt >= maxRetries) {
                    logger.error("Failed to send callback for event {} after {} attempts", 
                                event.getEventId(), maxRetries);
                } else {
                    // Wait before retry (exponential backoff)
                    try {
                        Thread.sleep(1000 * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        logger.warn("Callback retry interrupted for event {}", event.getEventId());
                        break;
                    }
                }
            }
        }
    }

    /**
     * Creates a callback request DTO from an event.
     */
    private CallbackRequest createCallbackRequest(Event event) {
        String status = event.getStatus() == EventStatus.COMPLETED ? "COMPLETED" : "FAILED";
        
        if (event.getStatus() == EventStatus.FAILED) {
            return new CallbackRequest(
                event.getEventId(),
                status,
                event.getEventType(),
                event.getErrorMessage(),
                event.getProcessedAt()
            );
        } else {
            return new CallbackRequest(
                event.getEventId(),
                status,
                event.getEventType(),
                event.getProcessedAt()
            );
        }
    }

    /**
     * Sends the actual HTTP POST request to the callback URL.
     */
    private void sendCallbackRequest(String callbackUrl, CallbackRequest callbackRequest) {
        try {
            // Set up headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Convert to JSON
            String jsonBody = objectMapper.writeValueAsString(callbackRequest);
            
            // Create request entity
            HttpEntity<String> requestEntity = new HttpEntity<>(jsonBody, headers);
            
            // Send POST request
            ResponseEntity<String> response = restTemplate.postForEntity(callbackUrl, requestEntity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.debug("Callback request successful for event {}: {}", 
                            callbackRequest.getEventId(), response.getStatusCode());
            } else {
                throw new RuntimeException("Callback returned non-success status: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to send callback: " + e.getMessage(), e);
        }
    }
}
