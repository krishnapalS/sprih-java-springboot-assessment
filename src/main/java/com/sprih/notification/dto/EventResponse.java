package com.sprih.notification.dto;

/**
 * DTO for event creation response.
 */
public class EventResponse {
    private String eventId;
    private String message;

    public EventResponse() {}

    public EventResponse(String eventId, String message) {
        this.eventId = eventId;
        this.message = message;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "EventResponse{" +
                "eventId='" + eventId + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
