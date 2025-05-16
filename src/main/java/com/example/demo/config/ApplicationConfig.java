package com.example.demo.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

class MicrosecondsLocalDateTimeDeserializer extends StdDeserializer<LocalDateTime> {
    // Default ISO_LOCAL_DATE_TIME formatter is usually sufficient for strings like "2025-05-16T10:14:25.491454"
    // For more complex patterns or sub-microsecond precision, a custom formatter might be needed.
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private final com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer defaultDeserializer = 
        new com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer(ISO_FORMATTER);


    public MicrosecondsLocalDateTimeDeserializer() {
        this(null);
    }
    public MicrosecondsLocalDateTimeDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public LocalDateTime deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        if (jp.currentToken() == JsonToken.VALUE_NUMBER_INT) {
            long microseconds = jp.getLongValue();
            long seconds = microseconds / 1_000_000L;
            long nanoOfSecond = (microseconds % 1_000_000L) * 1_000L;
            return LocalDateTime.ofInstant(Instant.ofEpochSecond(seconds, nanoOfSecond), ZoneOffset.UTC);
        } else if (jp.currentToken() == JsonToken.VALUE_STRING) {
            // If it's a string, use the default Jackson JSR310 deserializer for ISO_LOCAL_DATE_TIME
            // This handles various ISO-like string formats for LocalDateTime.
            return defaultDeserializer.deserialize(jp, ctxt);
        }
        return (LocalDateTime) ctxt.handleUnexpectedToken(LocalDateTime.class, jp);
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