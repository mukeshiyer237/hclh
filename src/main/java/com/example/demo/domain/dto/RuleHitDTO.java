package com.example.demo.domain.dto;

/**
 * Nested inside CreditScoreResponse — one entry per rule that fired.
 */
public record RuleHitDTO(
        String ruleCode,
        String ruleName,
        int pointsAwarded
) {}
