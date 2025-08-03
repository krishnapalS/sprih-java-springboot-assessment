package com.sprih.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sprih.notification.model.Event;
import com.sprih.notification.model.EventStatus;
import com.sprih.notification.model.EventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CallbackService.
 */
@ExtendWith(MockitoExtension.class)
class CallbackServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private CallbackService callbackService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        callbackService = new CallbackService();
        objectMapper = new ObjectMapper();
        
        // Inject mock RestTemplate
        ReflectionTestUtils.setField(callbackService, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(callbackService, "callbackTimeout", 5000);
        ReflectionTestUtils.setField(callbackService, "maxRetries", 3);
    }

    @Test
    void testSendCallbackForCompletedEvent() {
        // Arrange
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("recipient", "user@example.com");
        payload.put("message", "Test message");
        
        Event event = new Event("test-id", EventType.EMAIL, payload, "http://callback.url");
        event.setStatus(EventStatus.COMPLETED);
        event.setProcessedAt(Instant.now());

        ResponseEntity<String> mockResponse = new ResponseEntity<>("OK", HttpStatus.OK);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(mockResponse);

        // Act
        callbackService.sendCallback(event);

        // Assert
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        
        verify(restTemplate).postForEntity(urlCaptor.capture(), entityCaptor.capture(), eq(String.class));
        
        assertEquals("http://callback.url", urlCaptor.getValue());
        
        HttpEntity<String> capturedEntity = entityCaptor.getValue();
        String requestBody = capturedEntity.getBody();
        
        assertTrue(requestBody.contains("\"eventId\":\"test-id\""));
        assertTrue(requestBody.contains("\"status\":\"COMPLETED\""));
        assertTrue(requestBody.contains("\"eventType\":\"EMAIL\""));
        assertFalse(requestBody.contains("errorMessage"));
    }

    @Test
    void testSendCallbackForFailedEvent() {
        // Arrange
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("recipient", "user@example.com");
        payload.put("message", "Test message");
        
        Event event = new Event("test-id", EventType.EMAIL, payload, "http://callback.url");
        event.setStatus(EventStatus.FAILED);
        event.setErrorMessage("Simulated processing failure");
        event.setProcessedAt(Instant.now());

        ResponseEntity<String> mockResponse = new ResponseEntity<>("OK", HttpStatus.OK);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(mockResponse);

        // Act
        callbackService.sendCallback(event);

        // Assert
        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(anyString(), entityCaptor.capture(), eq(String.class));
        
        HttpEntity<String> capturedEntity = entityCaptor.getValue();
        String requestBody = capturedEntity.getBody();
        
        assertTrue(requestBody.contains("\"eventId\":\"test-id\""));
        assertTrue(requestBody.contains("\"status\":\"FAILED\""));
        assertTrue(requestBody.contains("\"eventType\":\"EMAIL\""));
        assertTrue(requestBody.contains("\"errorMessage\":\"Simulated processing failure\""));
    }

    @Test
    void testSendCallbackWithRetries() {
        // Arrange
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("recipient", "user@example.com");
        payload.put("message", "Test message");
        
        Event event = new Event("test-id", EventType.EMAIL, payload, "http://callback.url");
        event.setStatus(EventStatus.COMPLETED);
        event.setProcessedAt(Instant.now());

        // First two calls fail, third succeeds
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("Network error"))
                .thenThrow(new RuntimeException("Network error"))
                .thenReturn(new ResponseEntity<>("OK", HttpStatus.OK));

        // Act
        callbackService.sendCallback(event);

        // Assert
        verify(restTemplate, times(3)).postForEntity(anyString(), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void testSendCallbackWithNoCallbackUrl() {
        // Arrange
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("recipient", "user@example.com");
        payload.put("message", "Test message");
        
        Event event = new Event("test-id", EventType.EMAIL, payload, null);
        event.setStatus(EventStatus.COMPLETED);
        event.setProcessedAt(Instant.now());

        // Act
        callbackService.sendCallback(event);

        // Assert
        verify(restTemplate, never()).postForEntity(anyString(), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void testSendCallbackMaxRetriesExceeded() {
        // Arrange
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("recipient", "user@example.com");
        payload.put("message", "Test message");
        
        Event event = new Event("test-id", EventType.EMAIL, payload, "http://callback.url");
        event.setStatus(EventStatus.COMPLETED);
        event.setProcessedAt(Instant.now());

        // All calls fail
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("Network error"));

        // Act
        callbackService.sendCallback(event);

        // Assert - Should retry 3 times (maxRetries = 3)
        verify(restTemplate, times(3)).postForEntity(anyString(), any(HttpEntity.class), eq(String.class));
    }
}
