package com.example.demo.service;

import com.example.demo.domain.entity.CreditScoreRule;
import com.example.demo.exception.RuleEngineException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure calculation service — no DB access, no side effects.
 *
 * Evaluation logic:
 *   - Iterates all active rules in priority order (from the cache).
 *   - For each rule, checks whether the input falls within [min_value, max_value].
 *     NULL min means no lower bound; NULL max means no upper bound.
 *   - Accumulates score_points and records a RuleHit for every rule that fires.
 *   - INCOME rules are mutually exclusive by data design (non-overlapping ranges).
 *   - CARDS_HELD rules are independent and can stack with INCOME rules.
 *   - If no rule fires at all, throws RuleEngineException (seed data gap).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RuleEngineService {

    private final RuleCacheService ruleCacheService;

    /**
     * Evaluates the given inputs against all active rules.
     *
     * @return EvaluationResult containing total score and list of fired rule hits
     * @throws RuleEngineException if no rule matched the inputs
     */
    public EvaluationResult evaluate(BigDecimal annualSalary, int numberOfCreditCards) {
        List<CreditScoreRule> rules = ruleCacheService.getActiveRules();

        int totalScore = 0;
        List<RuleHit> hits = new ArrayList<>();

        for (CreditScoreRule rule : rules) {
            if (matches(rule, annualSalary, numberOfCreditCards)) {
                totalScore += rule.getScorePoints();
                hits.add(new RuleHit(rule, rule.getScorePoints()));
                log.debug("Rule [{}] fired: +{} points", rule.getRuleCode(), rule.getScorePoints());
            }
        }

        if (hits.isEmpty()) {
            throw new RuleEngineException(
                    String.format("No rule matched inputs: annualSalary=%s, numberOfCreditCards=%d",
                            annualSalary, numberOfCreditCards));
        }

        log.debug("Evaluation complete: totalScore={}, rulesHit={}", totalScore, hits.size());
        return new EvaluationResult(totalScore, hits);
    }

    // ── Internal range check ─────────────────────────────────────────────────────

    private boolean matches(CreditScoreRule rule, BigDecimal annualSalary, int numberOfCreditCards) {
        return switch (rule.getRuleCategory()) {
            case INCOME -> inRange(annualSalary, rule.getMinValue(), rule.getMaxValue());
            case CARDS_HELD -> inRange(BigDecimal.valueOf(numberOfCreditCards),
                    rule.getMinValue(), rule.getMaxValue());
        };
    }

    /**
     * Checks whether value falls within [min, max].
     * NULL min = no lower bound. NULL max = no upper bound.
     */
    private boolean inRange(BigDecimal value, BigDecimal min, BigDecimal max) {
        if (value == null) return false;
        if (min != null && value.compareTo(min) < 0) return false;
        if (max != null && value.compareTo(max) > 0) return false;
        return true;
    }

    // ── Value types returned to CreditScoringService ─────────────────────────────

    public record RuleHit(CreditScoreRule rule, int pointsAwarded) {}

    public record EvaluationResult(int totalScore, List<RuleHit> hits) {}
}
