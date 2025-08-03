package com.sprih.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main application class for the Event Notification System.
 * This application provides REST APIs for processing EMAIL, SMS, and PUSH notification events
 * with asynchronous processing and callback notifications.
 */
@SpringBootApplication
@EnableAsync
public class NotificationSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationSystemApplication.class, args);
    }
}
