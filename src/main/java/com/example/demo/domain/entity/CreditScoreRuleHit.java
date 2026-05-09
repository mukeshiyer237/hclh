package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Maps to credit_score_rule_hits.
 * One row per rule that fired during an evaluation.
 * Answers "why did this person get this score?" without relying on current rule state.
 */
@Getter
@Setter
@Entity
@Table(name = "credit_score_rule_hits")
public class CreditScoreRuleHit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hit_id")
    private Long hitId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "result_id", nullable = false)
    private CreditScoreResult creditScoreResult;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "rule_id", nullable = false)
    private CreditScoreRule creditScoreRule;

    @Column(name = "points_awarded", nullable = false)
    private Integer pointsAwarded;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
