package com.example.demo.kafka;

import com.example.demo.entity.Transaction;
import com.example.demo.service.TransactionService; // For RabbitMQ constants
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.JsonNode;

@Component
@Slf4j
@RequiredArgsConstructor
public class TransactionEventListener {

    private final ObjectMapper objectMapper;
    private final RabbitTemplate rabbitTemplate;

    // The topic should match Debezium's event router output for TRANSACTION_CREATED events.
    // Considering topic.prefix="app" and route.topic.replacement="${routedByValue}",
    // if routedByValue is "TRANSACTION_CREATED", topic is "app.TRANSACTION_CREATED"
    // However, the debezium config has route.topic.replacement = "${routedByValue}" which usually means the topic IS "TRANSACTION_CREATED"
    // We will use "TRANSACTION_CREATED" as per the outbox config: transforms.outbox.route.topic.replacement=\"${routedByValue}\"
    @KafkaListener(
        topics = "TRANSACTION_CREATED", // Topic name based on OutboxEvent.eventType
        groupId = "transaction-event-to-rabbitmq-group" // New group ID
    )
    public void listenToTransactionCreatedEvents(String message) {
        log.info("TransactionEventListener received message from Kafka topic TRANSACTION_CREATED: {}", message);
        try {
            // The message from Kafka (originating from OutboxEvent.payload via Debezium event router)
            // is expected to be the JSON string of the Transaction object.
            Transaction transaction = objectMapper.readValue(message, Transaction.class);
            log.info("Successfully deserialized Kafka message to Transaction: {}", transaction.getId());

            // Now, publish this transaction to RabbitMQ
            publishTransactionToRabbitMQ(transaction);

        } catch (Exception e) {
            log.error("Error processing Kafka message for TRANSACTION_CREATED event: {}. Error: {}", message, e.getMessage(), e);
            // Implement dead-letter queue (DLQ) or other error handling for Kafka messages if needed
        }
    }

    private void publishTransactionToRabbitMQ(Transaction transaction) {
        try {
            String transactionJson = objectMapper.writeValueAsString(transaction);
            rabbitTemplate.convertAndSend(
                TransactionService.TRANSACTION_EXCHANGE, 
                TransactionService.TRANSACTION_CREATED_ROUTING_KEY, 
                transactionJson
            );
            log.info("Published Transaction ID: {} to RabbitMQ exchange: {}, routingKey: {}", 
                transaction.getId(), TransactionService.TRANSACTION_EXCHANGE, TransactionService.TRANSACTION_CREATED_ROUTING_KEY);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize Transaction ID: {} for RabbitMQ.", transaction.getId(), e);
            // Handle RabbitMQ publishing error (e.g., retry, log, DLQ)
        } catch (Exception e) {
            log.error("An unexpected error occurred while publishing Transaction ID: {} to RabbitMQ.", transaction.getId(), e);
        }
    }

    // The old listener method for app.app.transactions can be removed or commented out
    // if it's no longer needed or if it was for a different purpose.
    
    @KafkaListener(
        topics = "bits.public.transactions",
        groupId = "transaction-consumer-group"
    )
    public void listen(String message) {
        log.info("TransactionEventListener received raw message (this is expected to be the Debezium payload directly): {}", message);
        try {
            JsonNode debeziumPayload = objectMapper.readTree(message); // Parse the incoming message string directly

            // No need to check for rootNode.hasNonNull("payload") anymore
            log.info("Parsed Debezium payload content: {}", debeziumPayload.toString());

            String op = debeziumPayload.path("op").asText(null); // Get 'op', default to null if not text
            log.info("Operation 'op': {}", op);

            if ("c".equals(op)) { // Check if it's a create operation
                if (debeziumPayload.hasNonNull("after")) {
                    JsonNode afterNode = debeziumPayload.path("after");
                    log.info("Raw 'after' data: {}", afterNode.toString());
                    try {
                        Transaction transaction = objectMapper.treeToValue(afterNode, Transaction.class);
                        log.info("Successfully mapped to Transaction entity: {}", transaction);
                        // Now you have the Transaction object, you can process it further
                        publishTransactionToRabbitMQ(transaction); // Added call to publish to RabbitMQ
                    } catch (Exception mappingException) {
                        log.error("Failed to map 'after' data to Transaction entity. 'after' data: {}. Error: {}", afterNode.toString(), mappingException.getMessage(), mappingException);
                    }
                } else {
                    log.warn("Create operation ('op'='c') found, but 'after' node is missing or null in Debezium payload: {}", debeziumPayload.toString());
                }
            } else {
                log.info("Skipping message as 'op' is not 'c'. Actual 'op': {}", op);
            }
        } catch (Exception e) {
            log.error("Error processing Kafka transaction message (top level): {}. Error: {}", message, e.getMessage(), e);
        }
    }
} 