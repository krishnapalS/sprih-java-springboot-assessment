package com.sprih.notification.service;

import com.sprih.notification.model.Event;
import com.sprih.notification.model.EventStatus;
import com.sprih.notification.model.EventType;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service responsible for processing events asynchronously.
 * Manages separate worker threads for each event type with configurable processing delays.
 */
@Service
public class EventProcessingService {
    
    private static final Logger logger = LoggerFactory.getLogger(EventProcessingService.class);
    
    @Value("${notification.processing.email-delay:5000}")
    private long emailDelay;
    
    @Value("${notification.processing.sms-delay:3000}")
    private long smsDelay;
    
    @Value("${notification.processing.push-delay:2000}")
    private long pushDelay;
    
    @Value("${notification.processing.failure-rate:0.1}")
    private double failureRate;
    
    @Value("${notification.shutdown.timeout:30000}")
    private long shutdownTimeout;
    
    @Autowired
    private CallbackService callbackService;
    
    private ExecutorService executorService;
    private final AtomicBoolean isShuttingDown = new AtomicBoolean(false);
    private final Random random = new Random();

    /**
     * Starts processing workers for each event type queue.
     */
    public void startProcessing(ConcurrentHashMap<EventType, BlockingQueue<Event>> queues) {
        // Create a fixed thread pool with one thread per event type
        executorService = Executors.newFixedThreadPool(EventType.values().length);
        
        // Start a worker thread for each event type
        for (EventType eventType : EventType.values()) {
            BlockingQueue<Event> queue = queues.get(eventType);
            executorService.submit(new EventProcessor(eventType, queue));
            logger.info("Started processing worker for event type: {}", eventType);
        }
    }

    /**
     * Worker class that processes events from a specific queue.
     */
    private class EventProcessor implements Runnable {
        private final EventType eventType;
        private final BlockingQueue<Event> queue;

        public EventProcessor(EventType eventType, BlockingQueue<Event> queue) {
            this.eventType = eventType;
            this.queue = queue;
        }

        @Override
        public void run() {
            logger.info("Event processor started for type: {}", eventType);
            
            while (!isShuttingDown.get()) {
                try {
                    // Poll for events with a timeout to allow shutdown checking
                    Event event = queue.poll(1, TimeUnit.SECONDS);
                    if (event != null) {
                        processEvent(event);
                    }
                } catch (InterruptedException e) {
                    // Thread interrupted, likely due to shutdown
                    logger.info("Event processor for {} interrupted", eventType);
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.error("Unexpected error in event processor for {}: {}", eventType, e.getMessage(), e);
                }
            }
            
            // Process remaining events during shutdown
            processRemainingEvents();
            logger.info("Event processor for {} shut down", eventType);
        }

        private void processRemainingEvents() {
            logger.info("Processing remaining events in {} queue during shutdown", eventType);
            Event event;
            while ((event = queue.poll()) != null) {
                processEvent(event);
            }
        }
    }

    /**
     * Processes a single event with simulated delay and failure.
     */
    private void processEvent(Event event) {
        logger.info("Starting processing of event {} (type: {})", event.getEventId(), event.getEventType());
        
        event.setStatus(EventStatus.PROCESSING);
        
        try {
            // Simulate processing delay based on event type
            long delay = getProcessingDelay(event.getEventType());
            Thread.sleep(delay);
            
            // Simulate random failure (10% chance)
            if (random.nextDouble() < failureRate) {
                throw new RuntimeException("Simulated processing failure");
            }
            
            // Mark as completed
            event.setStatus(EventStatus.COMPLETED);
            event.setProcessedAt(Instant.now());
            
            logger.info("Successfully processed event {} (type: {}) in {}ms", 
                       event.getEventId(), event.getEventType(), delay);
            
        } catch (InterruptedException e) {
            logger.warn("Event processing interrupted for event {}", event.getEventId());
            event.setStatus(EventStatus.FAILED);
            event.setErrorMessage("Processing interrupted");
            event.setProcessedAt(Instant.now());
            Thread.currentThread().interrupt();
            
        } catch (Exception e) {
            logger.error("Failed to process event {} (type: {}): {}", 
                        event.getEventId(), event.getEventType(), e.getMessage());
            event.setStatus(EventStatus.FAILED);
            event.setErrorMessage(e.getMessage());
            event.setProcessedAt(Instant.now());
        }
        
        // Send callback notification
        callbackService.sendCallback(event);
    }

    /**
     * Gets the processing delay for a specific event type.
     */
    private long getProcessingDelay(EventType eventType) {
        switch (eventType) {
            case EMAIL:
                return emailDelay;
            case SMS:
                return smsDelay;
            case PUSH:
                return pushDelay;
            default:
                return 1000; // Default 1 second
        }
    }

    /**
     * Initiates graceful shutdown of event processing.
     */
    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down event processing service");
        isShuttingDown.set(true);
        
        if (executorService != null) {
            executorService.shutdown();
            try {
                // Wait for existing tasks to terminate
                if (!executorService.awaitTermination(shutdownTimeout, TimeUnit.MILLISECONDS)) {
                    logger.warn("Event processing did not terminate gracefully within {}ms, forcing shutdown", shutdownTimeout);
                    executorService.shutdownNow();
                    
                    // Wait a bit more for tasks to respond to being cancelled
                    if (!executorService.awaitTermination(5000, TimeUnit.MILLISECONDS)) {
                        logger.error("Event processing did not terminate after forced shutdown");
                    }
                }
            } catch (InterruptedException e) {
                logger.warn("Shutdown interrupted, forcing immediate shutdown");
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        logger.info("Event processing service shutdown complete");
    }
}
