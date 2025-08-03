package com.sprih.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sprih.notification.model.Event;
import com.sprih.notification.model.EventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.BlockingQueue;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

/**
 * Unit tests for QueueService.
 */
@ExtendWith(MockitoExtension.class)
class QueueServiceTest {

    @Mock
    private EventProcessingService eventProcessingService;

    private QueueService queueService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        queueService = new QueueService();
        objectMapper = new ObjectMapper();
        
        // Use reflection to inject the mock
        try {
            java.lang.reflect.Field field = QueueService.class.getDeclaredField("eventProcessingService");
            field.setAccessible(true);
            field.set(queueService, eventProcessingService);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        doNothing().when(eventProcessingService).startProcessing(any());
        queueService.initializeQueues();
    }

    @Test
    void testAddEmailEventToQueue() {
        // Arrange
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("recipient", "user@example.com");
        payload.put("message", "Test message");
        
        Event event = new Event("test-id", EventType.EMAIL, payload, "http://callback.url");

        // Act
        queueService.addEvent(event);

        // Assert
        assertEquals(1, queueService.getQueueSize(EventType.EMAIL));
        assertEquals(0, queueService.getQueueSize(EventType.SMS));
        assertEquals(0, queueService.getQueueSize(EventType.PUSH));
    }

    @Test
    void testAddSmsEventToQueue() {
        // Arrange
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("phoneNumber", "+911234567890");
        payload.put("message", "Test SMS");
        
        Event event = new Event("test-id", EventType.SMS, payload, "http://callback.url");

        // Act
        queueService.addEvent(event);

        // Assert
        assertEquals(0, queueService.getQueueSize(EventType.EMAIL));
        assertEquals(1, queueService.getQueueSize(EventType.SMS));
        assertEquals(0, queueService.getQueueSize(EventType.PUSH));
    }

    @Test
    void testAddPushEventToQueue() {
        // Arrange
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("deviceId", "device-123");
        payload.put("message", "Test push notification");
        
        Event event = new Event("test-id", EventType.PUSH, payload, "http://callback.url");

        // Act
        queueService.addEvent(event);

        // Assert
        assertEquals(0, queueService.getQueueSize(EventType.EMAIL));
        assertEquals(0, queueService.getQueueSize(EventType.SMS));
        assertEquals(1, queueService.getQueueSize(EventType.PUSH));
    }

    @Test
    void testFifoOrdering() {
        // Arrange
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("recipient", "user@example.com");
        payload.put("message", "Test message");
        
        Event event1 = new Event("event-1", EventType.EMAIL, payload, "http://callback.url");
        Event event2 = new Event("event-2", EventType.EMAIL, payload, "http://callback.url");
        Event event3 = new Event("event-3", EventType.EMAIL, payload, "http://callback.url");

        // Act
        queueService.addEvent(event1);
        queueService.addEvent(event2);
        queueService.addEvent(event3);

        // Assert
        BlockingQueue<Event> emailQueue = queueService.getQueue(EventType.EMAIL);
        assertEquals("event-1", emailQueue.poll().getEventId());
        assertEquals("event-2", emailQueue.poll().getEventId());
        assertEquals("event-3", emailQueue.poll().getEventId());
    }

    @Test
    void testGetTotalQueueSize() {
        // Arrange
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("recipient", "user@example.com");
        payload.put("message", "Test message");
        
        Event emailEvent = new Event("email-1", EventType.EMAIL, payload, "http://callback.url");
        Event smsEvent = new Event("sms-1", EventType.SMS, payload, "http://callback.url");
        Event pushEvent = new Event("push-1", EventType.PUSH, payload, "http://callback.url");

        // Act
        queueService.addEvent(emailEvent);
        queueService.addEvent(smsEvent);
        queueService.addEvent(pushEvent);

        // Assert
        assertEquals(3, queueService.getTotalQueueSize());
    }

    @Test
    void testShutdownPreventsNewEvents() {
        // Act
        queueService.initiateShutdown();

        // Assert
        assertTrue(queueService.isShuttingDown());
        
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("recipient", "user@example.com");
        payload.put("message", "Test message");
        
        Event event = new Event("test-id", EventType.EMAIL, payload, "http://callback.url");
        
        assertThrows(IllegalStateException.class, () -> queueService.addEvent(event));
    }
}
