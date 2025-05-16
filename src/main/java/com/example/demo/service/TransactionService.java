package com.example.demo.service;

import com.example.demo.entity.OutboxEvent;
import com.example.demo.entity.Transaction;
import com.example.demo.repository.OutboxEventRepository;
import com.example.demo.repository.TransactionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public static final String TRANSACTION_EXCHANGE = "transaction-exchange";
    public static final String TRANSACTION_CREATED_ROUTING_KEY = "transaction.created";
    public static final String TRANSACTION_CREATED_EVENT_TYPE = "TRANSACTION_CREATED";

    @Transactional(readOnly = true)
    public List<Transaction> findAll() {
        return transactionRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Transaction findById(Integer id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found with id: " + id));
    }

    @Transactional
    public Transaction create(Transaction transaction) {
        Transaction savedTransaction = transactionRepository.save(transaction);
        // createOutboxEvent("TRANSACTION", savedTransaction.getId().toString(), TRANSACTION_CREATED_EVENT_TYPE, savedTransaction);
        return savedTransaction;
    }

    public void createBulkTransactions(int count) {
        for (int i = 0; i < count; i++) {
            Transaction transaction = Transaction.builder()
                    .amount(BigDecimal.valueOf(Math.random() * 1000))
                    .type("BULK_DEPOSIT_" + UUID.randomUUID().toString())
                    .description("Bulk transaction " + i)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            transactionRepository.save(transaction);
            log.info("Saved transaction {} of {}", i + 1, count);
        }
        log.info("Finished creating {} bulk transactions and corresponding outbox events.", count);
    }

    private void createOutboxEvent(String aggregateType, String aggregateId, String eventType, Object payload) {
        try {
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .payload(objectMapper.writeValueAsString(payload))
                    .build();
            outboxEventRepository.save(outboxEvent);
            log.info("Created outbox event: {} for aggregateId: {}", eventType, aggregateId);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize payload for outbox event", e);
            throw new RuntimeException("Failed to create outbox event for " + aggregateType + " id " + aggregateId, e);
        }
    }
} 