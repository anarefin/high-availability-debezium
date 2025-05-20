# Outbox Pattern POC: Debezium → Kafka → RabbitMQ

This project demonstrates two approaches for Change Data Capture (CDC): 
- The Outbox Pattern (for Product changes) using Debezium to capture PostgreSQL changes, Kafka as the event bus, and RabbitMQ as the final message broker.
- Direct CDC (for Transactions) from PostgreSQL, also likely utilizing Debezium and Kafka.

## Architecture

The flow of events is as follows:

**1. Outbox Pattern for Product Changes:**

1. Product changes (create/update/delete) are saved in PostgreSQL.
2. Events are written to an outbox table in the same transaction.
3. Debezium captures changes from the outbox table and publishes to Kafka.
4. A Kafka consumer receives the events and forwards them to RabbitMQ.
5. A RabbitMQ consumer processes the events.

**2. Direct CDC for Transactions:**

1. Transaction data is created/updated in the `Transactions` table in PostgreSQL.
2. Debezium (or a similar CDC mechanism) captures these changes directly from the `Transactions` table.
3. These change events are published to a Kafka topic.
4. A Kafka consumer processes these transaction events, potentially forwarding them to RabbitMQ or another downstream service.
5. A final consumer processes these transaction events.

## Components

- **PostgreSQL**: Stores the product data, outbox events, and transaction data.
- **Debezium**: CDC (Change Data Capture) tool used for both outbox and direct table capture.
- **Kafka**: Event bus for Debezium events
- **Spring Boot**: Application framework
- **RabbitMQ**: Message broker for final event consumption

## Setup and Run

### Prerequisites

- Docker and Docker Compose
- Java 21 (ensure this version is installed and configured)
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

1. Create a product using the REST API (demonstrates Outbox Pattern):

```bash
curl -X POST -H "Content-Type: application/json" \
    -d '{"name":"New Product","description":"Test product","price":49.99}' \
    http://localhost:8080/api/products
```

2. Check the logs to see (for Product changes):
   - The product being saved to the database
   - The outbox event being created
   - Debezium capturing the event and publishing to Kafka
   - The Kafka consumer receiving the event
   - The event being forwarded to RabbitMQ
   - The RabbitMQ consumer processing the event

3. For `Transactions` table (demonstrates Direct CDC):
   - Trigger an operation that adds or modifies a record in the `Transactions` table (e.g., via an API endpoint if available, or direct database interaction).
   - Check the logs or Kafka topics to observe:
     - Debezium capturing the direct change from the `Transactions` table.
     - The event being published to the relevant Kafka topic.
     - Subsequent consumers processing this transaction event.

## Management UIs

- RabbitMQ Management: http://localhost:15672 (guest/guest)
- Kafka has no UI in this setup, use CLI tools for monitoring

## Notes on the Outbox Pattern

The Outbox Pattern is a reliable way to implement event-driven architectures with eventual consistency, solving the "dual-write" problem by:

- Ensuring atomicity of database changes and event creation
- Allowing for exactly-once event delivery
- Decoupling event publishing from event processing

Debezium's outbox event router SMT (Single Message Transform) is specifically designed to work with this pattern for data like Products in this project.

**Direct CDC for Transactions Table**

This project also captures changes directly from a `Transactions` table. This approach can be simpler to implement if the strict atomicity provided by the outbox pattern isn't a critical requirement for transaction data, or if the goal is to compare different CDC strategies. It relies on the database's transaction log and a CDC tool like Debezium to stream changes as they happen. 