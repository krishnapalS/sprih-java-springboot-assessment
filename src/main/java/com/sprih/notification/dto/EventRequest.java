package com.sprih.notification.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.sprih.notification.model.EventType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO for incoming event requests via REST API.
 */
public class EventRequest {
    
    @NotNull(message = "Event type is required")
    private EventType eventType;
    
    @NotNull(message = "Payload is required")
    private JsonNode payload;
    
    @NotBlank(message = "Callback URL is required")
    private String callbackUrl;

    public EventRequest() {}

    public EventRequest(EventType eventType, JsonNode payload, String callbackUrl) {
        this.eventType = eventType;
        this.payload = payload;
        this.callbackUrl = callbackUrl;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public JsonNode getPayload() {
        return payload;
    }

    public void setPayload(JsonNode payload) {
        this.payload = payload;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    @Override
    public String toString() {
        return "EventRequest{" +
                "eventType=" + eventType +
                ", callbackUrl='" + callbackUrl + '\'' +
                '}';
    }
}
