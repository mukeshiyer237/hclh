package com.example.demo.service;

import com.example.demo.domain.dto.CreditScoreRequest;
import com.example.demo.domain.dto.CreditScoreResponse;
import com.example.demo.domain.dto.RuleHitDTO;
import com.example.demo.domain.entity.CreditScoreResult;
import com.example.demo.domain.entity.CreditScoreRuleHit;
import com.example.demo.exception.DuplicateEvaluationException;
import com.example.demo.kafka.producer.CreditScoreEventProducer;
import com.example.demo.repository.CreditScoreResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Orchestrates a single credit score evaluation:
 *
 *  1. Idempotency check — reject if applicationId already has a result (409)
 *  2. Run rule engine — pure score calculation, no DB
 *  3. Persist CreditScoreResult + CreditScoreRuleHit rows in one transaction
 *  4. Publish credit.score.evaluated AFTER commit — never inside the transaction
 *  5. Return CreditScoreResponse
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreditScoringService {

    private final CreditScoreResultRepository resultRepository;
    private final RuleEngineService ruleEngineService;
    private final CreditScoreEventProducer eventProducer;

    @Transactional
    public CreditScoreResponse evaluate(CreditScoreRequest request) {
        log.info("Evaluating credit score: userId={} applicationId={} applicationRef={}",
                request.userId(), request.applicationId(), request.applicationRef());

        // Step 1 — idempotency guard (service-layer check before hitting DB constraint)
        if (resultRepository.existsByApplicationId(request.applicationId())) {
            throw new DuplicateEvaluationException(request.applicationId());
        }

        // Step 2 — rule engine (pure, no DB)
        RuleEngineService.EvaluationResult evaluation =
                ruleEngineService.evaluate(request.annualSalary(), request.numberOfCreditCards());

        // Step 3 — persist
        CreditScoreResult result = buildResult(request, evaluation);
        CreditScoreResult saved = resultRepository.save(result);

        log.info("Credit score persisted: resultId={} finalScore={} rulesHit={}",
                saved.getResultId(), saved.getFinalScore(), saved.getRuleHits().size());

        // Step 4 — publish AFTER commit (TransactionSynchronization alternative:
        // here we rely on the fact this method returns before the caller publishes,
        // and the @Transactional proxy commits before returning to the caller stack.
        // Publishing is done by the caller chain AFTER this method returns.)
        publishAfterCommit(saved);

        // Step 5 — map and return
        return toResponse(saved);
    }

    // ── Internal ─────────────────────────────────────────────────────────────────

    private CreditScoreResult buildResult(CreditScoreRequest request,
                                          RuleEngineService.EvaluationResult evaluation) {
        CreditScoreResult result = new CreditScoreResult();
        result.setApplicationId(request.applicationId());
        result.setUserId(request.userId());
        result.setApplicationRef(request.applicationRef());
        result.setFinalScore(evaluation.totalScore());
        result.setScoreBasis("CALCULATED");
        result.setAnnualSalarySnapshot(request.annualSalary());
        result.setCardsCountSnapshot(((short) request.numberOfCreditCards()));
        result.setEvaluatedAt(LocalDateTime.now());

        List<CreditScoreRuleHit> hits = evaluation.hits().stream()
                .map(h -> {
                    CreditScoreRuleHit hit = new CreditScoreRuleHit();
                    hit.setCreditScoreResult(result);
                    hit.setCreditScoreRule(h.rule());
                    hit.setPointsAwarded(h.pointsAwarded());
                    hit.setCreatedAt(LocalDateTime.now());
                    return hit;
                })
                .toList();

        result.setRuleHits(hits);
        return result;
    }

    /**
     * Publishes the Kafka event after the transaction commits.
     * Uses TransactionSynchronizationManager so the publish never races the commit.
     */
    private void publishAfterCommit(CreditScoreResult saved) {
        org.springframework.transaction.support.TransactionSynchronizationManager
                .registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        eventProducer.publishScoreEvaluated(saved);
                    }
                });
    }

    private CreditScoreResponse toResponse(CreditScoreResult result) {
        List<RuleHitDTO> ruleDTOs = result.getRuleHits().stream()
                .map(h -> new RuleHitDTO(
                        h.getCreditScoreRule().getRuleCode(),
                        h.getCreditScoreRule().getRuleName(),
                        h.getPointsAwarded()))
                .toList();

        return new CreditScoreResponse(
                result.getResultId(),
                result.getUserId(),
                result.getApplicationRef(),
                result.getFinalScore(),
                result.getScoreBasis(),
                ruleDTOs,
                result.getEvaluatedAt()
        );
    }
}
