package com.example.demo.kafka.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Consumed from credit.score.requested.
 * Published by application-service when it wants async scoring.
 */
public record CreditScoreRequestedEvent(
        String eventId,
        String eventType,
        LocalDateTime occurredAt,
        Long applicationId,
        String applicationRef,
        Long userId,
        String username,
        BigDecimal annualSalary,
        int numberOfCreditCards
) {}
