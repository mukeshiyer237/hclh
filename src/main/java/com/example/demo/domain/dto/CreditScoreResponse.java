package com.example.demo.domain.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Outbound response for POST /internal/credit-scores
 * and GET /internal/credit-scores/{userId} endpoints.
 */
public record CreditScoreResponse(
        Long resultId,
        Long userId,
        String applicationRef,
        int finalScore,
        String scoreBasis,
        List<RuleHitDTO> rulesApplied,
        LocalDateTime evaluatedAt
) {}
