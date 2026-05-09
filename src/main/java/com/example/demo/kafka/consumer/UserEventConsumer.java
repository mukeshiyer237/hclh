package com.example.demo.kafka.consumer;

import com.example.demo.audit.UserLifecycleEvent;
import com.example.demo.audit.UserLifecycleEventRepository;
import com.example.demo.kafka.event.UserCreatedEvent;
import com.example.demo.kafka.event.UserDeletedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Consumes events from users-topic and writes audit rows to user_lifecycle_events.
 *
 * This consumer is audit-only. Credit scoring is a separate domain — it is
 * triggered independently via POST /internal/credit-scores (sync) or the
 * credit.score.requested Kafka topic (async). User identity here is for
 * internal RBAC only and has no relation to the credit scoring customerId.
 *
 * Bad messages are logged and acknowledged — a poison pill must never stall the partition.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventConsumer {

    private final UserLifecycleEventRepository repository;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${kafka.topics.users}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(String message) {
        log.debug("Received message from users-topic: {}", message);
        try {
            JsonNode node = objectMapper.readTree(message);

            if (node.has("email") && !node.has("deletedAt")) {
                handleCreated(message, node);
            } else if (node.has("deletedAt")) {
                handleDeleted(message, node);
            } else {
                log.warn("Unrecognised event shape on users-topic, skipping: {}", message);
            }
        } catch (Exception ex) {
            log.error("Failed to process users-topic message, skipping: {}", ex.getMessage(), ex);
        }
    }

    private void handleCreated(String raw, JsonNode node) throws JsonProcessingException {
        UserCreatedEvent event = objectMapper.treeToValue(node, UserCreatedEvent.class);
        UserLifecycleEvent record = UserLifecycleEvent.builder()
                .userId(event.userId())
                .username(event.username())
                .eventType("USER_CREATED")
                .payload(raw)
                .occurredAt(event.createdAt() != null ? event.createdAt() : LocalDateTime.now())
                .build();
        repository.save(record);
        log.info("Audit recorded: USER_CREATED userId={} username={}", event.userId(), event.username());
    }

    private void handleDeleted(String raw, JsonNode node) throws JsonProcessingException {
        UserDeletedEvent event = objectMapper.treeToValue(node, UserDeletedEvent.class);
        UserLifecycleEvent record = UserLifecycleEvent.builder()
                .userId(event.userId())
                .username(event.username())
                .eventType("USER_DELETED")
                .payload(raw)
                .occurredAt(event.deletedAt() != null ? event.deletedAt() : LocalDateTime.now())
                .build();
        repository.save(record);
        log.info("Audit recorded: USER_DELETED userId={} username={}", event.userId(), event.username());
    }
}
