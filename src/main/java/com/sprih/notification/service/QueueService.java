package com.sprih.notification.service;

import com.sprih.notification.model.Event;
import com.sprih.notification.model.EventType;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service responsible for managing separate queues for each event type.
 * Provides thread-safe queue operations and FIFO processing.
 */
@Service
public class QueueService {
    
    private static final Logger logger = LoggerFactory.getLogger(QueueService.class);
    
    private final ConcurrentHashMap<EventType, BlockingQueue<Event>> queues = new ConcurrentHashMap<>();
    private final AtomicBoolean isShuttingDown = new AtomicBoolean(false);
    
    @Autowired
    private EventProcessingService eventProcessingService;

    @PostConstruct
    public void initializeQueues() {
        // Initialize separate queues for each event type
        for (EventType eventType : EventType.values()) {
            queues.put(eventType, new LinkedBlockingQueue<>());
            logger.info("Initialized queue for event type: {}", eventType);
        }
        
        // Start processing workers for each queue
        eventProcessingService.startProcessing(queues);
    }

    /**
     * Adds an event to the appropriate queue based on its type.
     * 
     * @param event The event to be added to the queue
     * @throws IllegalStateException if the system is shutting down
     */
    public void addEvent(Event event) {
        if (isShuttingDown.get()) {
            throw new IllegalStateException("System is shutting down. Not accepting new events.");
        }
        
        BlockingQueue<Event> queue = queues.get(event.getEventType());
        if (queue == null) {
            throw new IllegalArgumentException("Unsupported event type: " + event.getEventType());
        }
        
        try {
            queue.put(event);
            logger.info("Event {} added to {} queue. Queue size: {}", 
                       event.getEventId(), event.getEventType(), queue.size());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while adding event to queue", e);
        }
    }

    /**
     * Gets the queue for a specific event type.
     */
    public BlockingQueue<Event> getQueue(EventType eventType) {
        return queues.get(eventType);
    }

    /**
     * Gets the current size of a specific queue.
     */
    public int getQueueSize(EventType eventType) {
        BlockingQueue<Event> queue = queues.get(eventType);
        return queue != null ? queue.size() : 0;
    }

    /**
     * Gets the total number of events across all queues.
     */
    public int getTotalQueueSize() {
        return queues.values().stream()
                .mapToInt(BlockingQueue::size)
                .sum();
    }

    /**
     * Initiates graceful shutdown by stopping acceptance of new events.
     */
    public void initiateShutdown() {
        logger.info("Initiating graceful shutdown. Stopping acceptance of new events.");
        isShuttingDown.set(true);
    }

    /**
     * Checks if the system is currently shutting down.
     */
    public boolean isShuttingDown() {
        return isShuttingDown.get();
    }

    @PreDestroy
    public void shutdown() {
        logger.info("QueueService shutdown initiated");
        initiateShutdown();
    }
}
