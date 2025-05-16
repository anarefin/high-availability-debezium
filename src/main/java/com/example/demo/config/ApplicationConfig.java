package com.example.demo.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

class MicrosecondsLocalDateTimeDeserializer extends StdDeserializer<LocalDateTime> {
    public MicrosecondsLocalDateTimeDeserializer() {
        this(null);
    }
    public MicrosecondsLocalDateTimeDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public LocalDateTime deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        long microseconds = jp.getLongValue();
        long seconds = microseconds / 1_000_000L; // Use L for long literal
        long nanoOfSecond = (microseconds % 1_000_000L) * 1_000L; // Convert micros to nanos, use L
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(seconds, nanoOfSecond), ZoneOffset.UTC); // Assuming UTC for conversion
    }
}

@Configuration
public class ApplicationConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addDeserializer(LocalDateTime.class, new MicrosecondsLocalDateTimeDeserializer());
        objectMapper.registerModule(javaTimeModule);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper;
    }
} 