package com.example.demo.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class OutboxEventListener {

    private final ObjectMapper objectMapper;
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.name:product-events-exchange}")
    private String exchangeName;

    @Value("${rabbitmq.routing.key:product-events}")
    private String routingKey;

    // Listen to both the general outbox events topic and specific event topics
    @KafkaListener(
        topics = {
            "${kafka.topics.outbox-events:app.outbox_events}",
            "PRODUCT_CREATED",
            "PRODUCT_UPDATED",
            "PRODUCT_DELETED"
        },
        groupId = "${spring.kafka.consumer.group-id:outbox-consumer-group}"
    )
    public void listen(String message) {
        log.info("Received Kafka message: {}", message);

        try {
            // Try to process as a Debezium outbox event format
            JsonNode jsonNode = objectMapper.readTree(message);
            
            // Check if this is a Debezium message (has a payload.after structure)
            if (jsonNode.has("payload") && jsonNode.path("payload").has("after")) {
                // Extract the relevant parts from the Debezium message
                JsonNode payload = jsonNode.path("payload");
                JsonNode after = payload.path("after");
                
                // Extract the outbox event data
                String eventType = after.path("event_type").asText();
                String aggregateType = after.path("aggregate_type").asText();
                String eventPayload = after.path("payload").asText();
                
                // Log the extracted data
                log.info("Extracted Debezium event - Type: {}, Aggregate: {}, Payload: {}", 
                        eventType, aggregateType, eventPayload);
                
                // Create a routing key based on the event type
                String specificRoutingKey = routingKey + "." + eventType.toLowerCase();
                
                // Forward the message to RabbitMQ
                rabbitTemplate.convertAndSend(exchangeName, specificRoutingKey, eventPayload);
                log.info("Forwarded Debezium event to RabbitMQ - Exchange: {}, Routing Key: {}", 
                        exchangeName, specificRoutingKey);
                return;
            } else {
                // It's a direct message from a specific topic
                // Forward directly to RabbitMQ
                String topicName = "product-events.direct";
                rabbitTemplate.convertAndSend(exchangeName, topicName, message);
                log.info("Forwarded direct message to RabbitMQ - Exchange: {}, Routing Key: {}", 
                        exchangeName, topicName);
            }
        } catch (JsonProcessingException e) {
            log.error("Error processing Kafka message: {}", message, e);
        } catch (Exception e) {
            log.error("Unexpected error processing message: {}", message, e);
        }
    }
} 