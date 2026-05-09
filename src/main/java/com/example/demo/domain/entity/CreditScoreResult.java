package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Maps to credit_score_results.
 * Written once per application evaluation — never updated.
 * The UNIQUE constraint on application_id is the idempotency guarantee.
 */
@Getter
@Setter
@Entity
@Immutable
@Table(name = "credit_score_results")
public class CreditScoreResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "result_id")
    private Long resultId;

    @Column(name = "application_id", nullable = false, unique = true)
    private Long applicationId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "application_ref", nullable = false, length = 100)
    private String applicationRef;

    @Column(name = "final_score", nullable = false)
    private Integer finalScore;

    @Column(name = "score_basis", nullable = false, length = 20)
    private String scoreBasis;

    /** Snapshot of annualSalary at evaluation time — immutable audit record. */
    @Column(name = "annual_salary_snapshot", nullable = false, precision = 15, scale = 2)
    private BigDecimal annualSalarySnapshot;

    /** Snapshot of numberOfCreditCards at evaluation time — immutable audit record. */
    @Column(name = "cards_count_snapshot", nullable = false)
    private Short cardsCountSnapshot;

    @Column(name = "evaluated_at", nullable = false)
    private LocalDateTime evaluatedAt;

    @OneToMany(mappedBy = "creditScoreResult", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<CreditScoreRuleHit> ruleHits = new ArrayList<>();
}
