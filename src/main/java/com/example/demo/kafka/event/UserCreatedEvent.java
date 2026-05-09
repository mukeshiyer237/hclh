package com.example.demo.kafka.event;

import java.time.LocalDateTime;

/**
 * Published by the middleware/user service when a new user is onboarded.
 * Used here for audit purposes only — credit scoring is triggered independently
 * via POST /internal/credit-scores or credit.score.requested topic.
 */
public record UserCreatedEvent(
        Long userId,
        String username,
        String email,
        LocalDateTime createdAt
) {}
