package com.sprih.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sprih.notification.model.Event;
import com.sprih.notification.model.EventStatus;
import com.sprih.notification.model.EventType;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for EventProcessingService.
 */
@ExtendWith(MockitoExtension.class)
class EventProcessingServiceTest {

    @Mock
    private CallbackService callbackService;

    private EventProcessingService eventProcessingService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        eventProcessingService = new EventProcessingService();
        objectMapper = new ObjectMapper();
        
        // Inject mocks and test configuration
        ReflectionTestUtils.setField(eventProcessingService, "callbackService", callbackService);
        ReflectionTestUtils.setField(eventProcessingService, "emailDelay", 100L); // Faster for testing
        ReflectionTestUtils.setField(eventProcessingService, "smsDelay", 50L);
        ReflectionTestUtils.setField(eventProcessingService, "pushDelay", 25L);
        ReflectionTestUtils.setField(eventProcessingService, "failureRate", 0.0); // No failures for basic tests
        ReflectionTestUtils.setField(eventProcessingService, "shutdownTimeout", 5000L);

        // Mock callback service - using lenient for shutdown test
        lenient().doNothing().when(callbackService).sendCallback(any());
    }

    @Test
    void testEventProcessingWithDifferentDelays() {
        // Arrange
        ConcurrentHashMap<EventType, BlockingQueue<Event>> queues = createQueues();
        
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("message", "Test message");
        
        Event emailEvent = new Event("email-1", EventType.EMAIL, payload, "http://callback.url");
        Event smsEvent = new Event("sms-1", EventType.SMS, payload, "http://callback.url");
        Event pushEvent = new Event("push-1", EventType.PUSH, payload, "http://callback.url");

        // Act
        eventProcessingService.startProcessing(queues);
        
        queues.get(EventType.EMAIL).offer(emailEvent);
        queues.get(EventType.SMS).offer(smsEvent);
        queues.get(EventType.PUSH).offer(pushEvent);

        // Assert - Wait for processing to complete
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .until(() -> emailEvent.getStatus() == EventStatus.COMPLETED);
        
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .until(() -> smsEvent.getStatus() == EventStatus.COMPLETED);
        
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .until(() -> pushEvent.getStatus() == EventStatus.COMPLETED);

        assertEquals(EventStatus.COMPLETED, emailEvent.getStatus());
        assertEquals(EventStatus.COMPLETED, smsEvent.getStatus());
        assertEquals(EventStatus.COMPLETED, pushEvent.getStatus());
        
        assertNotNull(emailEvent.getProcessedAt());
        assertNotNull(smsEvent.getProcessedAt());
        assertNotNull(pushEvent.getProcessedAt());
    }

    @Test
    void testEventProcessingWithFailures() {
        // Arrange - Set high failure rate for this test
        ReflectionTestUtils.setField(eventProcessingService, "failureRate", 1.0); // 100% failure
        
        ConcurrentHashMap<EventType, BlockingQueue<Event>> queues = createQueues();
        
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("message", "Test message");
        
        Event event = new Event("fail-test", EventType.PUSH, payload, "http://callback.url");

        // Act
        eventProcessingService.startProcessing(queues);
        queues.get(EventType.PUSH).offer(event);

        // Assert
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .until(() -> event.getStatus() == EventStatus.FAILED);

        assertEquals(EventStatus.FAILED, event.getStatus());
        assertNotNull(event.getErrorMessage());
        assertNotNull(event.getProcessedAt());
    }

    @Test
    void testGracefulShutdown() {
        // Arrange
        ConcurrentHashMap<EventType, BlockingQueue<Event>> queues = createQueues();
        eventProcessingService.startProcessing(queues);

        // Act
        eventProcessingService.shutdown();

        // Assert - Should complete without hanging
        assertTrue(true); // If we reach here, shutdown completed
    }

    private ConcurrentHashMap<EventType, BlockingQueue<Event>> createQueues() {
        ConcurrentHashMap<EventType, BlockingQueue<Event>> queues = new ConcurrentHashMap<>();
        for (EventType eventType : EventType.values()) {
            queues.put(eventType, new LinkedBlockingQueue<>());
        }
        return queues;
    }
}
