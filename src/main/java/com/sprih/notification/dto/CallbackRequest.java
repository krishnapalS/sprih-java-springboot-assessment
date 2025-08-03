package com.sprih.notification.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sprih.notification.model.EventType;

import java.time.Instant;

/**
 * DTO for callback notifications sent to client systems.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CallbackRequest {
    private String eventId;
    private String status;
    private EventType eventType;
    private String errorMessage;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant processedAt;

    public CallbackRequest() {}

    public CallbackRequest(String eventId, String status, EventType eventType, Instant processedAt) {
        this.eventId = eventId;
        this.status = status;
        this.eventType = eventType;
        this.processedAt = processedAt;
    }

    public CallbackRequest(String eventId, String status, EventType eventType, String errorMessage, Instant processedAt) {
        this.eventId = eventId;
        this.status = status;
        this.eventType = eventType;
        this.errorMessage = errorMessage;
        this.processedAt = processedAt;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }

    @Override
    public String toString() {
        return "CallbackRequest{" +
                "eventId='" + eventId + '\'' +
                ", status='" + status + '\'' +
                ", eventType=" + eventType +
                ", errorMessage='" + errorMessage + '\'' +
                ", processedAt=" + processedAt +
                '}';
    }
}
