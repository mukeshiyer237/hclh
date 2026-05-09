package com.example.demo.repository;

import com.example.demo.domain.entity.CreditScoreRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CreditScoreRuleRepository extends JpaRepository<CreditScoreRule, Long> {

    /** Load all active rules ordered by priority — used by RuleCacheService on startup/refresh. */
    List<CreditScoreRule> findByIsActiveTrueOrderByPriorityOrderAsc();
}
