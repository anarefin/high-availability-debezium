package com.example.demo.service;

import com.example.demo.entity.OutboxEvent;
import com.example.demo.entity.Product;
import com.example.demo.repository.OutboxEventRepository;
import com.example.demo.repository.ProductRepository;
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
public class ProductService {

    private final ProductRepository productRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Product findById(Integer id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
    }

    @Transactional
    public Product create(Product product) {
        Product savedProduct = productRepository.save(product);
        
        // Create an outbox event for the created product
        createOutboxEvent("PRODUCT", savedProduct.getId().toString(), "PRODUCT_CREATED", savedProduct);
        
        return savedProduct;
    }

    @Transactional
    public Product update(Integer id, Product productDetails) {
        Product existingProduct = findById(id);
        
        // Update the product fields
        existingProduct.setName(productDetails.getName());
        existingProduct.setDescription(productDetails.getDescription());
        existingProduct.setPrice(productDetails.getPrice());
        
        Product updatedProduct = productRepository.save(existingProduct);
        
        // Create an outbox event for the updated product
        createOutboxEvent("PRODUCT", updatedProduct.getId().toString(), "PRODUCT_UPDATED", updatedProduct);
        
        return updatedProduct;
    }

    @Transactional
    public void delete(Integer id) {
        Product product = findById(id);
        productRepository.delete(product);
        
        // Create an outbox event for the deleted product
        createOutboxEvent("PRODUCT", id.toString(), "PRODUCT_DELETED", 
                          new DeleteEvent(id, "Product deleted"));
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


    // Simple DTO for delete events
    private record DeleteEvent(Integer id, String message) {}
} 