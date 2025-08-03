package com.sprih.notification.controller;

import com.sprih.notification.dto.EventRequest;
import com.sprih.notification.dto.EventResponse;
import com.sprih.notification.model.Event;
import com.sprih.notification.service.QueueService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for handling event notification requests.
 * Provides the main API endpoint for submitting events to the notification system.
 */
@RestController
@RequestMapping("/api")
public class EventController {
    
    private static final Logger logger = LoggerFactory.getLogger(EventController.class);
    
    private final QueueService queueService;

    @Autowired
    public EventController(QueueService queueService) {
        this.queueService = queueService;
    }

    /**
     * Accepts new events for processing.
     * 
     * @param eventRequest The event request containing event type, payload, and callback URL
     * @param bindingResult Validation results
     * @return EventResponse with event ID and confirmation message
     */
    @PostMapping("/events")
    public ResponseEntity<?> createEvent(@Valid @RequestBody EventRequest eventRequest, 
                                       BindingResult bindingResult) {
        
        logger.info("Received event request: {}", eventRequest);
        
        // Check for validation errors
        if (bindingResult.hasErrors()) {
            StringBuilder errorMessage = new StringBuilder("Validation failed: ");
            bindingResult.getFieldErrors().forEach(error -> 
                errorMessage.append(error.getField()).append(" - ").append(error.getDefaultMessage()).append("; ")
            );
            logger.warn("Validation failed for event request: {}", errorMessage.toString());
            return ResponseEntity.badRequest().body(errorMessage.toString());
        }

        // Validate payload based on event type
        String validationError = validateEventPayload(eventRequest);
        if (validationError != null) {
            logger.warn("Payload validation failed: {}", validationError);
            return ResponseEntity.badRequest().body(validationError);
        }

        try {
            // Generate unique event ID
            String eventId = UUID.randomUUID().toString();
            
            // Create event
            Event event = new Event(eventId, eventRequest.getEventType(), 
                                  eventRequest.getPayload(), eventRequest.getCallbackUrl());
            
            // Add to appropriate queue
            queueService.addEvent(event);
            
            logger.info("Event {} added to {} queue successfully", eventId, eventRequest.getEventType());
            
            // Return response
            EventResponse response = new EventResponse(eventId, "Event accepted for processing.");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            logger.error("Error processing event request: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal server error: " + e.getMessage());
        }
    }

    /**
     * Validates event payload based on event type requirements.
     */
    private String validateEventPayload(EventRequest eventRequest) {
        if (eventRequest.getPayload() == null || !eventRequest.getPayload().isObject()) {
            return "Payload must be a valid JSON object";
        }

        switch (eventRequest.getEventType()) {
            case EMAIL:
                if (!eventRequest.getPayload().has("recipient") || 
                    !eventRequest.getPayload().has("message")) {
                    return "EMAIL event payload must contain 'recipient' and 'message' fields";
                }
                break;
            case SMS:
                if (!eventRequest.getPayload().has("phoneNumber") || 
                    !eventRequest.getPayload().has("message")) {
                    return "SMS event payload must contain 'phoneNumber' and 'message' fields";
                }
                break;
            case PUSH:
                if (!eventRequest.getPayload().has("deviceId") || 
                    !eventRequest.getPayload().has("message")) {
                    return "PUSH event payload must contain 'deviceId' and 'message' fields";
                }
                break;
            default:
                return "Unsupported event type: " + eventRequest.getEventType();
        }
        
        return null; // No validation errors
    }

    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Event Notification System is running");
    }
}
