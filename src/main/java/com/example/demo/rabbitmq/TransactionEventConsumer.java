package com.example.demo.rabbitmq;

import com.example.demo.entity.RabbitTransaction;
import com.example.demo.entity.Transaction;
import com.example.demo.repository.RabbitTransactionRepository;
import com.example.demo.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionEventConsumer {

    private final RabbitTransactionRepository rabbitTransactionRepository;
    private final ObjectMapper objectMapper;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "${rabbitmq.queue.transaction-created:transaction-created-queue}", durable = "true"),
            exchange = @Exchange(
                    value = TransactionService.TRANSACTION_EXCHANGE,
                    type = ExchangeTypes.TOPIC,
                    durable = "true"
            ),
            key = TransactionService.TRANSACTION_CREATED_ROUTING_KEY
    ))
    @Transactional
    public void receiveTransactionMessage(String message) {
        log.info("Received transaction message from RabbitMQ: {}", message);
        try {
            Transaction originalTransaction = objectMapper.readValue(message, Transaction.class);

            RabbitTransaction rabbitTransaction = RabbitTransaction.builder()
                    .originalTransactionId(originalTransaction.getId())
                    .amount(originalTransaction.getAmount())
                    .type(originalTransaction.getType())
                    .description(originalTransaction.getDescription())
                    .createdAt(originalTransaction.getCreatedAt())
                    .receivedAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            rabbitTransactionRepository.save(rabbitTransaction);
            log.info("Saved RabbitTransaction from original transaction ID: {}", originalTransaction.getId());

        } catch (Exception e) {
            log.error("Error processing transaction message from RabbitMQ: {}", message, e);
        }
    }
} 