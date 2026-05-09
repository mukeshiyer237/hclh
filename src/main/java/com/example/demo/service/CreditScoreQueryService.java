package com.example.demo.service;

import com.example.demo.domain.dto.CreditScoreResponse;
import com.example.demo.domain.dto.RuleHitDTO;
import com.example.demo.domain.entity.CreditScoreResult;
import com.example.demo.exception.ScoreNotFoundException;
import com.example.demo.repository.CreditScoreResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Read-only service for GET endpoints.
 * No writes, no side effects.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreditScoreQueryService {

    private final CreditScoreResultRepository resultRepository;

    /** Returns the most recent credit score result for a user. */
    @Transactional(readOnly = true)
    public CreditScoreResponse getLatestByUserId(Long userId) {
        List<CreditScoreResult> results =
                resultRepository.findByUserIdOrderByEvaluatedAtDesc(userId);

        if (results.isEmpty()) {
            throw new ScoreNotFoundException(userId);
        }

        return toResponse(results.get(0));
    }

    /** Returns full scoring history for a user, newest first. */
    @Transactional(readOnly = true)
    public List<CreditScoreResponse> getHistoryByUserId(Long userId) {
        List<CreditScoreResult> results =
                resultRepository.findByUserIdOrderByEvaluatedAtDesc(userId);

        if (results.isEmpty()) {
            throw new ScoreNotFoundException(userId);
        }

        return results.stream().map(this::toResponse).toList();
    }

    // ── Mapping ──────────────────────────────────────────────────────────────────

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
