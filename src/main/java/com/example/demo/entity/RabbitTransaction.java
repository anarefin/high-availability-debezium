package com.example.demo.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "rabbit_transactions", schema = "app")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RabbitTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "serial")
    private Integer id;

    // Adding a field to store the original transaction ID if needed for tracing
    @Column(name = "original_transaction_id")
    private Integer originalTransactionId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String type;

    private String description;

    @JsonProperty("created_at")
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // We can add a received_at timestamp for when it was processed from RabbitMQ
    @JsonProperty("received_at")
    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;


    @PrePersist
    protected void onCreate() {
        if (createdAt == null) { // If createdAt is not set from the original transaction
            createdAt = LocalDateTime.now();
        }
        if (receivedAt == null) {
            receivedAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
} 