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

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

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
        //createOutboxEvent("TRANSACTION", savedTransaction.getId().toString(), "TRANSACTION_CREATED", savedTransaction);
        return savedTransaction;
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
            log.info("Created outbox event: {}", outboxEvent);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize payload", e);
            throw new RuntimeException("Failed to create outbox event", e);
        }
    }
} 