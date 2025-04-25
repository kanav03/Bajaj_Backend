# Bajaj Finserv Health Backend Assignment

This is a Spring Boot application that processes webhooks and solves graph-based questions based on user relationships.

## Features

- Webhook processing for user data
- Graph algorithms for finding mutual followers and nth level followers
- Reactive programming with WebFlux
- Automatic retry mechanism for rate limiting
- Error handling and logging

## Prerequisites

- Java 11 or higher
- Maven 3.6 or higher

## Building and Running

To build the project:
```bash
mvn clean package
```

To run the application:
```bash
java -jar target/webhook-processor-1.0.0.jar
```

## Configuration

The application can be configured through `src/main/resources/application.properties`:

- `server.port`: The port on which the application runs (default: 8080)
- `webhook.base.url`: The base URL for the webhook API
- `logging.level.com.bajaj`: Debug level for application logs

## Author

Kanav Nijhawan 