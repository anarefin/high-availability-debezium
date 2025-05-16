package com.example.demo.repository;

import com.example.demo.entity.RabbitTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RabbitTransactionRepository extends JpaRepository<RabbitTransaction, Integer> {
} 