# Event Notification System

A Java Spring Boot application that provides asynchronous event processing for EMAIL, SMS, and PUSH notifications with callback support and graceful shutdown handling.

## Features

- **REST API**: Accept EMAIL, SMS, and PUSH notification events via `/api/events` endpoint
- **Separate Queues**: Each event type is processed in its own dedicated queue (FIFO)
- **Asynchronous Processing**: Events are processed with simulated delays:
  - EMAIL: 5 seconds
  - SMS: 3 seconds
  - PUSH: 2 seconds
- **Failure Simulation**: 10% of events randomly fail to simulate real-world scenarios
- **Callback Notifications**: Automatic HTTP POST callbacks to client systems upon completion/failure
- **Graceful Shutdown**: System stops accepting new events and completes existing ones during shutdown
- **Thread Safety**: Concurrent processing with thread-safe data structures
- **Comprehensive Testing**: JUnit tests covering all major functionality

## Quick Start with Docker

To start the application using Docker:

```bash
docker compose up
```

The API will be available at: `http://localhost:8080/api/events`

## Development Setup

### Prerequisites
- Java 17+
- Maven 3.6+

### Running Locally
```bash
# Clone the repository
git clone <your-repository-url>
cd event-notification-system

# Run tests
mvn test

# Start the application
mvn spring-boot:run
```

The API will be available at: `http://localhost:5000/api/events`

## API Specification

### Create Event
**POST** `/api/events`

**Request Body:**
```json
{
    "eventType": "EMAIL|SMS|PUSH",
    "payload": {
        // Event-specific payload (see examples below)
    },
    "callbackUrl": "http://client-system.com/api/event-status"
}
```

**Response:**
```json
{
    "eventId": "e123",
    "message": "Event accepted for processing."
}
```

### Event Type Examples

#### EMAIL Event
```json
{
    "eventType": "EMAIL",
    "payload": {
        "recipient": "user@example.com",
        "message": "Welcome to our service!"
    },
    "callbackUrl": "http://client-system.com/api/event-status"
}
```

#### SMS Event
```json
{
    "eventType": "SMS", 
    "payload": {
        "phoneNumber": "+911234567890",
        "message": "Your OTP is 123456"
    },
    "callbackUrl": "http://client-system.com/api/event-status"
}
```

#### PUSH Event
```json
{
    "eventType": "PUSH",
    "payload": {
        "deviceId": "abc-123-xyz",
        "message": "Your order has been shipped!"
    },
    "callbackUrl": "http://client-system.com/api/event-status"
}
```

### Health Check
**GET** `/api/health`

Returns: `Event Notification System is running`

### Callback Notifications

Upon completion or failure, the system sends a POST request to the provided callback URL:

**Success Callback:**
```json
{
    "eventId": "e123",
    "status": "COMPLETED",
    "eventType": "EMAIL",
    "processedAt": "2025-08-03T06:41:00Z"
}
```

**Failure Callback:**
```json
{
    "eventId": "e123",
    "status": "FAILED",
    "eventType": "EMAIL",
    "errorMessage": "Simulated processing failure",
    "processedAt": "2025-08-03T06:41:00Z"
}
```

## Testing the API

### Test EMAIL Event
```bash
curl -X POST http://localhost:8080/api/events \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "EMAIL",
    "payload": {
      "recipient": "user@example.com",
      "message": "Welcome to our service!"
    },
    "callbackUrl": "http://httpbin.org/post"
  }'
```

### Test SMS Event  
```bash
curl -X POST http://localhost:8080/api/events \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "SMS",
    "payload": {
      "phoneNumber": "+911234567890",
      "message": "Your OTP is 123456"
    },
    "callbackUrl": "http://httpbin.org/post"
  }'
```

### Test PUSH Event
```bash
curl -X POST http://localhost:8080/api/events \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "PUSH",
    "payload": {
      "deviceId": "abc-123-xyz",
      "message": "Your order has been shipped!"
    },
    "callbackUrl": "http://httpbin.org/post"
  }'
```

## Running Tests

```bash
# Run all tests
mvn test

# Run tests with coverage
mvn test jacoco:report
```

## Architecture Overview

- **Controller Layer**: REST API endpoints for event submission
- **Service Layer**: Event queue management and asynchronous processing
- **Queue Management**: Separate FIFO queues for each event type
- **Thread Pool**: Dedicated worker threads for each event type
- **Callback System**: HTTP client for notifying completion status
- **Graceful Shutdown**: Ensures all queued events complete before termination

## Configuration

Key configuration properties in `application.yml`:

```yaml
notification:
  processing:
    email-delay: 5000    # EMAIL processing delay (ms)
    sms-delay: 3000      # SMS processing delay (ms) 
    push-delay: 2000     # PUSH processing delay (ms)
    failure-rate: 0.1    # Random failure rate (10%)
  callback:
    timeout: 5000        # Callback request timeout (ms)
    retries: 3           # Maximum retry attempts
  shutdown:
    timeout: 30000       # Graceful shutdown timeout (ms)
```

## Docker Deployment

The application includes production-ready Docker configuration:

- **Multi-stage build** for optimized image size
- **Non-root user** for security
- **Health checks** for container monitoring
- **JVM tuning** for production workloads
- **Graceful shutdown** handling

## Assumptions

- Callback URLs are publicly accessible HTTP endpoints
- Events are processed in FIFO order within each queue type
- 10% random failure rate simulates real-world processing failures
- All events must complete during graceful shutdown (max 30 seconds)
- Network connectivity is available for callback notifications

## Notes

This implementation demonstrates:
- Clean separation of concerns with Spring Boot
- Thread-safe concurrent processing
- Proper error handling and retry logic
- Comprehensive unit test coverage
- Production-ready Docker containerization
- RESTful API design with validation
