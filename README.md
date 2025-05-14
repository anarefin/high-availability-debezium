# Outbox Pattern POC: Debezium → Kafka → RabbitMQ

This project demonstrates the implementation of the Outbox Pattern using:
- Debezium to capture PostgreSQL changes
- Kafka as the event bus
- RabbitMQ as the final message broker

## Architecture

The flow of events is as follows:

1. Product changes (create/update/delete) are saved in PostgreSQL
2. Events are written to an outbox table in the same transaction
3. Debezium captures changes from the outbox table and publishes to Kafka
4. A Kafka consumer receives the events and forwards them to RabbitMQ
5. A RabbitMQ consumer processes the events

## Components

- **PostgreSQL**: Stores the product data and outbox events
- **Debezium**: CDC (Change Data Capture) tool 
- **Kafka**: Event bus for Debezium events
- **Spring Boot**: Application framework
- **RabbitMQ**: Message broker for final event consumption

## Setup and Run

### Prerequisites

- Docker and Docker Compose
- Java 21
- Gradle

### Start the Infrastructure

```bash
# Start all required services
docker-compose up -d
```

### Register the Debezium Connector

```bash
# Wait for Kafka Connect to be available, then register the connector
curl -X POST -H "Content-Type: application/json" \
    --data @debezium-connector-config.json \
    http://localhost:8083/connectors
```

### Build and Run the Application

```bash
./gradlew build
./gradlew bootRun
```

## Testing the Flow

1. Create a product using the REST API:

```bash
curl -X POST -H "Content-Type: application/json" \
    -d '{"name":"New Product","description":"Test product","price":49.99}' \
    http://localhost:8080/api/products
```

2. Check the logs to see:
   - The product being saved to the database
   - The outbox event being created
   - Debezium capturing the event and publishing to Kafka
   - The Kafka consumer receiving the event
   - The event being forwarded to RabbitMQ
   - The RabbitMQ consumer processing the event

## Management UIs

- RabbitMQ Management: http://localhost:15672 (guest/guest)
- Kafka has no UI in this setup, use CLI tools for monitoring

## Notes on the Outbox Pattern

The Outbox Pattern is a reliable way to implement event-driven architectures with eventual consistency, solving the "dual-write" problem by:

- Ensuring atomicity of database changes and event creation
- Allowing for exactly-once event delivery
- Decoupling event publishing from event processing

Debezium's outbox event router SMT (Single Message Transform) is specifically designed to work with this pattern. 