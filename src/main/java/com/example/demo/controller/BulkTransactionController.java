package com.example.demo.controller;

import com.example.demo.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bulk-transactions")
@RequiredArgsConstructor
public class BulkTransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<String> createBulkTransactions(@RequestParam(defaultValue = "100000") int count) {
        transactionService.createBulkTransactions(count);
        return ResponseEntity.ok("Started creating " + count + " bulk transactions.");
    }
} 