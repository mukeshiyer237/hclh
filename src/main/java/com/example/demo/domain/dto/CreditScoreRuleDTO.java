package com.example.demo.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Read-only view of a credit_score_rules row.
 * Returned by GET /internal/credit-score-rules.
 */
public record CreditScoreRuleDTO(
        Long ruleId,
        String ruleCode,
        String ruleName,
        String ruleCategory,
        BigDecimal minValue,
        BigDecimal maxValue,
        int scorePoints,
        int priorityOrder,
        boolean isActive,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
