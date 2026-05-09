package com.example.demo.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Inbound payload for POST /internal/credit-scores.
 */
public record CreditScoreRequest(

        @NotNull(message = "userId is required")
        Long userId,

        /** Optional — used for logging only, not stored. */
        String username,

        @NotNull(message = "applicationId is required")
        Long applicationId,

        @NotNull(message = "applicationRef is required")
        String applicationRef,

        @NotNull(message = "annualSalary is required")
        @DecimalMin(value = "0.01", message = "annualSalary must be greater than 0")
        BigDecimal annualSalary,

        @Min(value = 0, message = "numberOfCreditCards must be 0 or more")
        int numberOfCreditCards
) {}
