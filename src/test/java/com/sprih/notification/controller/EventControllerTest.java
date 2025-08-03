package com.sprih.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sprih.notification.dto.EventRequest;
import com.sprih.notification.model.EventType;
import com.sprih.notification.service.QueueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for EventController REST API endpoints.
 */
@WebMvcTest(EventController.class)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QueueService queueService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testCreateEmailEvent_Success() throws Exception {
        // Arrange
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("recipient", "user@example.com");
        payload.put("message", "Welcome to our service!");

        EventRequest request = new EventRequest(EventType.EMAIL, payload, "http://callback.url");
        
        doNothing().when(queueService).addEvent(any());

        // Act & Assert
        mockMvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventId").exists())
                .andExpect(jsonPath("$.message").value("Event accepted for processing."));

        verify(queueService).addEvent(any());
    }

    @Test
    void testCreateSmsEvent_Success() throws Exception {
        // Arrange
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("phoneNumber", "+911234567890");
        payload.put("message", "Your OTP is 123456");

        EventRequest request = new EventRequest(EventType.SMS, payload, "http://callback.url");
        
        doNothing().when(queueService).addEvent(any());

        // Act & Assert
        mockMvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventId").exists());

        verify(queueService).addEvent(any());
    }

    @Test
    void testCreatePushEvent_Success() throws Exception {
        // Arrange
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("deviceId", "abc-123-xyz");
        payload.put("message", "Your order has been shipped!");

        EventRequest request = new EventRequest(EventType.PUSH, payload, "http://callback.url");
        
        doNothing().when(queueService).addEvent(any());

        // Act & Assert
        mockMvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(queueService).addEvent(any());
    }

    @Test
    void testCreateEvent_MissingEventType() throws Exception {
        // Arrange
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("recipient", "user@example.com");
        payload.put("message", "Test message");

        String requestBody = "{\"payload\":" + payload.toString() + ",\"callbackUrl\":\"http://callback.url\"}";

        // Act & Assert
        mockMvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateEvent_MissingCallbackUrl() throws Exception {
        // Arrange
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("recipient", "user@example.com");
        payload.put("message", "Test message");

        EventRequest request = new EventRequest(EventType.EMAIL, payload, null);

        // Act & Assert
        mockMvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateEvent_InvalidEmailPayload() throws Exception {
        // Arrange
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("recipient", "user@example.com");
        // Missing message field

        EventRequest request = new EventRequest(EventType.EMAIL, payload, "http://callback.url");

        // Act & Assert
        mockMvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("EMAIL event payload must contain")));
    }

    @Test
    void testCreateEvent_InvalidSmsPayload() throws Exception {
        // Arrange
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("phoneNumber", "+911234567890");
        // Missing message field

        EventRequest request = new EventRequest(EventType.SMS, payload, "http://callback.url");

        // Act & Assert
        mockMvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("SMS event payload must contain")));
    }

    @Test
    void testCreateEvent_InvalidPushPayload() throws Exception {
        // Arrange
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("deviceId", "abc-123-xyz");
        // Missing message field

        EventRequest request = new EventRequest(EventType.PUSH, payload, "http://callback.url");

        // Act & Assert
        mockMvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("PUSH event payload must contain")));
    }

    @Test
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Event Notification System is running"));
    }
}
