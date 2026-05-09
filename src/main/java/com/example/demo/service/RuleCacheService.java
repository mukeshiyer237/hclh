package com.example.demo.service;

import com.example.demo.domain.dto.CreditScoreRuleDTO;
import com.example.demo.domain.entity.CreditScoreRule;
import com.example.demo.repository.CreditScoreRuleRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * In-memory cache of active credit scoring rules.
 *
 * Rules are loaded once at startup and refreshed every 5 minutes automatically.
 * Manual refresh is available via the controller endpoint (PUT /internal/credit-score-rules/refresh).
 *
 * ReadWriteLock semantics: concurrent reads are non-blocking;
 * refresh acquires an exclusive write lock for the duration of the list swap only.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RuleCacheService {

    private final CreditScoreRuleRepository ruleRepository;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private volatile List<CreditScoreRule> cachedRules = Collections.emptyList();

    // ── Lifecycle ────────────────────────────────────────────────────────────────

    @PostConstruct
    public void init() {
        log.info("RuleCacheService: initial rule load");
        loadRules();
    }

    /** Auto-refresh every 5 minutes. Requires @EnableScheduling on a config class. */
    @Scheduled(fixedRateString = "${credit.rules.cache.refresh-ms:300000}")
    public void scheduledRefresh() {
        log.debug("RuleCacheService: scheduled refresh triggered");
        loadRules();
    }

    // ── Public API ───────────────────────────────────────────────────────────────

    /**
     * Returns active rules sorted by priority_order.
     * Called by RuleEngineService on every evaluation — O(1) read path.
     */
    public List<CreditScoreRule> getActiveRules() {
        lock.readLock().lock();
        try {
            return cachedRules;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns ALL rules (active + inactive) as DTOs.
     * Called by GET /internal/credit-score-rules — hits DB directly, not the cache.
     */
    public List<CreditScoreRuleDTO> getAllRules() {
        return ruleRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Manual cache refresh — called by PUT /internal/credit-score-rules/refresh.
     * Returns the count of active rules now loaded and the refresh timestamp.
     */
    public int refresh() {
        loadRules();
        return getActiveRules().size();
    }

    // ── Internal ─────────────────────────────────────────────────────────────────

    private void loadRules() {
        List<CreditScoreRule> fresh = ruleRepository.findByIsActiveTrueOrderByPriorityOrderAsc();

        lock.writeLock().lock();
        try {
            cachedRules = Collections.unmodifiableList(fresh);
        } finally {
            lock.writeLock().unlock();
        }

        log.info("RuleCacheService: loaded {} active rules", fresh.size());
    }

    private CreditScoreRuleDTO toDTO(CreditScoreRule rule) {
        return new CreditScoreRuleDTO(
                rule.getRuleId(),
                rule.getRuleCode(),
                rule.getRuleName(),
                rule.getRuleCategory().name(),
                rule.getMinValue(),
                rule.getMaxValue(),
                rule.getScorePoints(),
                rule.getPriorityOrder(),
                rule.getIsActive(),
                rule.getDescription(),
                rule.getCreatedAt(),
                rule.getUpdatedAt()
        );
    }
}
