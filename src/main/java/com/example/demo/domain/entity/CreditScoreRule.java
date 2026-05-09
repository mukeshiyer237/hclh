package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Maps to credit_score_rules.
 * Loaded and cached by RuleCacheService on startup — not modified at runtime.
 */
@Getter
@Setter
@Entity
@Table(name = "credit_score_rules")
public class CreditScoreRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rule_id")
    private Long ruleId;

    @Column(name = "rule_code", nullable = false, unique = true, length = 100)
    private String ruleCode;

    @Column(name = "rule_name", nullable = false, length = 255)
    private String ruleName;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_category", nullable = false, length = 20)
    private RuleCategory ruleCategory;

    /** Inclusive lower bound. NULL means no lower bound. */
    @Column(name = "min_value", precision = 15, scale = 2)
    private BigDecimal minValue;

    /** Inclusive upper bound. NULL means no upper bound. */
    @Column(name = "max_value", precision = 15, scale = 2)
    private BigDecimal maxValue;

    @Column(name = "score_points", nullable = false)
    private Integer scorePoints;

    @Column(name = "priority_order", nullable = false)
    private Integer priorityOrder;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum RuleCategory {
        INCOME,
        CARDS_HELD
    }
}
