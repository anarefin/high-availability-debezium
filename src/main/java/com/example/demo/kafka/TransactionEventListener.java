package com.example.demo.kafka;

import com.example.demo.entity.Transaction;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class TransactionEventListener {

    private final ObjectMapper objectMapper;

    @KafkaListener(
        topics = "app.app.transactions",
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