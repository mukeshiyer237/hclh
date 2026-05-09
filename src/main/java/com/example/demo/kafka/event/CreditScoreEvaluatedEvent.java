package com.example.demo.kafka.event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Published to credit.score.evaluated after every successful evaluation.
 * Partition key is userId — all events for the same user go to the same partition.
 */
public record CreditScoreEvaluatedEvent(
        String eventId,
        String eventType,
        LocalDateTime occurredAt,
        Long applicationId,
        String applicationRef,
        Long userId,
        int finalScore,
        String scoreBasis,
        List<RuleApplied> rulesApplied
) {
    public record RuleApplied(String ruleCode, int pointsAwarded) {}

    /** Factory — builds the event from primitive fields. */
    public static CreditScoreEvaluatedEvent of(
            Long applicationId,
            String applicationRef,
            Long userId,
            int finalScore,
            String scoreBasis,
            List<RuleApplied> rulesApplied
    ) {
        return new CreditScoreEvaluatedEvent(
                UUID.randomUUID().toString(),
                "CREDIT_SCORE_EVALUATED",
                LocalDateTime.now(),
                applicationId,
                applicationRef,
                userId,
                finalScore,
                scoreBasis,
                rulesApplied
        );
    }
}
