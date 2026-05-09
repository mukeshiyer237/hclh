package com.example.demo.kafka.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Single entry point for all Kafka publishing.
 * Payload is serialized to a JSON string before sending so the wire format
 * is always a UTF-8 JSON string regardless of producer serializer config.
 */
@Slf4j
@Service
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Primary constructor — Spring uses this.
     * Registers JavaTimeModule so LocalDateTime serializes correctly.
     */
    @Autowired
    public KafkaProducerService(KafkaTemplate<String, String> kafkaTemplate) {
        this(kafkaTemplate, new ObjectMapper().findAndRegisterModules());
    }

    /**
     * Test constructor — allows injecting a custom/mock ObjectMapper.
     */
    public KafkaProducerService(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper  = objectMapper;
    }

    /**
     * Publish a message without a partition key (Kafka round-robins across partitions).
     */
    public void publish(String topic, Object payload) {
        publish(topic, null, payload);
    }

    /**
     * Publish a message with an explicit key.
     * Messages with the same key always go to the same partition — use this
     * when ordering matters for a given entity (e.g. userId as key).
     */
    public void publish(String topic, String key, Object payload) {
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize payload for topic [{}]: {}", topic, ex.getMessage(), ex);
            return;
        }

        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(topic, key, json);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish to topic [{}] key [{}]: {}", topic, key, ex.getMessage(), ex);
            } else {
                log.debug("Published to topic [{}] partition [{}] offset [{}]",
                        topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
