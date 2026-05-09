package com.example.demo.domain.dto;

import java.time.LocalDateTime;

/**
 * Returned by PUT /internal/credit-score-rules/refresh.
 */
public record RuleCacheRefreshResponse(
        int rulesLoaded,
        LocalDateTime refreshedAt
) {}
