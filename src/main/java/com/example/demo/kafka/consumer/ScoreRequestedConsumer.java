package com.example.demo.kafka.consumer;

import com.example.demo.domain.dto.CreditScoreRequest;
import com.example.demo.exception.DuplicateEvaluationException;
import com.example.demo.kafka.event.CreditScoreRequestedEvent;
import com.example.demo.service.CreditScoringService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Async path for credit score evaluation.
 *
 * Consumes credit.score.requested events published by application-service
 * during saga retries or bulk re-scoring flows.
 * Runs the same CreditScoringService.evaluate() logic as the HTTP path.
 *
 * Error handling:
 *   - DuplicateEvaluationException → log and acknowledge (expected on retries, not a DLT candidate)
 *   - All other exceptions → propagate so Kafka retries and eventually routes to DLT
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScoreRequestedConsumer {

    private final CreditScoringService creditScoringService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${kafka.topics.credit-score-requested:credit.score.requested}",
            groupId = "${kafka.consumer.credit-scoring-group:credit-scoring-service-group}"
    )
    public void consume(String message) {
        CreditScoreRequestedEvent event;
        try {
            event = objectMapper.readValue(message, CreditScoreRequestedEvent.class);
        } catch (Exception ex) {
            log.error("Failed to deserialize CreditScoreRequestedEvent — sending to DLT. payload=[{}]",
                    message, ex);
            throw new RuntimeException("Deserialization failure", ex);
        }

        log.info("Received credit.score.requested: eventId={} applicationId={} userId={}",
                event.eventId(), event.applicationId(), event.userId());

        CreditScoreRequest request = new CreditScoreRequest(
                event.userId(),
                event.username(),
                event.applicationId(),
                event.applicationRef(),
                event.annualSalary(),
                event.numberOfCreditCards()
        );

        try {
            creditScoringService.evaluate(request);
            log.info("Async evaluation complete for applicationId={}", event.applicationId());
        } catch (DuplicateEvaluationException ex) {
            // Expected on Kafka retries — result already exists, safe to acknowledge
            log.warn("Duplicate evaluation via Kafka — already processed: applicationId={}. Acknowledging.",
                    event.applicationId());
        }
        // All other exceptions propagate → Kafka retries → DLT after max retries
    }
}
