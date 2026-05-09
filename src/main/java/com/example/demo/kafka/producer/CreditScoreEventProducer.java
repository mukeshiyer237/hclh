package com.example.demo.kafka.producer;

import com.example.demo.domain.entity.CreditScoreResult;
import com.example.demo.domain.entity.CreditScoreRuleHit;
import com.example.demo.kafka.event.CreditScoreEvaluatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Publishes credit.score.evaluated events after successful evaluations.
 * Always called AFTER the DB transaction commits — never inside a transaction.
 * Partition key is userId so all events for the same user are ordered.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreditScoreEventProducer {

    private final KafkaProducerService kafkaProducerService;

    @Value("${kafka.topics.credit-score-evaluated:credit.score.evaluated}")
    private String topic;

    public void publishScoreEvaluated(CreditScoreResult result) {
        List<CreditScoreEvaluatedEvent.RuleApplied> rulesApplied = result.getRuleHits().stream()
                .map(hit -> new CreditScoreEvaluatedEvent.RuleApplied(
                        hit.getCreditScoreRule().getRuleCode(),
                        hit.getPointsAwarded()))
                .toList();

        CreditScoreEvaluatedEvent event = CreditScoreEvaluatedEvent.of(
                result.getApplicationId(),
                result.getApplicationRef(),
                result.getUserId(),
                result.getFinalScore(),
                result.getScoreBasis(),
                rulesApplied
        );

        String partitionKey = result.getUserId().toString();

        try {
            kafkaProducerService.publish(topic, partitionKey, event);
            log.info("Published credit.score.evaluated for userId={} applicationId={}",
                    result.getUserId(), result.getApplicationId());
        } catch (Exception ex) {
            // Result is already persisted — caller has their response.
            // Log full payload so it can be manually re-published if needed.
            log.error("Failed to publish credit.score.evaluated for userId={} applicationId={}. " +
                            "Event payload: eventId={} finalScore={}",
                    result.getUserId(), result.getApplicationId(),
                    event.eventId(), event.finalScore(), ex);
        }
    }
}
