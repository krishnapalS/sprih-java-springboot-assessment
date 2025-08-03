package com.sprih.notification.config;

import com.sprih.notification.service.QueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PreDestroy;

/**
 * Configuration for handling graceful shutdown of the notification system.
 */
@Configuration
public class ShutdownConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(ShutdownConfig.class);
    
    @Autowired
    private QueueService queueService;

    /**
     * Handles graceful shutdown by ensuring all events in queues are processed.
     */
    @PreDestroy
    public void handleShutdown() {
        logger.info("Application shutdown initiated - stopping acceptance of new events");
        queueService.initiateShutdown();
        
        // Wait for queues to be processed
        int maxWaitTime = 30; // seconds
        int waitTime = 0;
        
        while (queueService.getTotalQueueSize() > 0 && waitTime < maxWaitTime) {
            try {
                Thread.sleep(1000);
                waitTime++;
                logger.info("Waiting for {} events to complete processing ({}s elapsed)", 
                           queueService.getTotalQueueSize(), waitTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Shutdown wait interrupted");
                break;
            }
        }
        
        if (queueService.getTotalQueueSize() > 0) {
            logger.warn("Shutdown timeout reached with {} events still in queues", 
                       queueService.getTotalQueueSize());
        } else {
            logger.info("All events processed successfully during shutdown");
        }
    }
}
