package com.example.demo.rabbitmq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ProductEventConsumer {

    @RabbitListener(bindings = @QueueBinding(
        value = @Queue("${rabbitmq.queue.name:product-events-queue}"),
        exchange = @Exchange(
            value = "${rabbitmq.exchange.name:product-events-exchange}", 
            type = ExchangeTypes.TOPIC
        ),
        key = {"${rabbitmq.routing.key:product-events}.#"}
    ))
    public void receiveMessage(String message) {
        log.info("Received message from RabbitMQ: {}", message);
        // Process the message as needed
        // In a real application, you might update a cache, notify other services, etc.
    }
} 