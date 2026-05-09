package com.example.demo.repository;

import com.example.demo.domain.entity.CreditScoreResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CreditScoreResultRepository extends JpaRepository<CreditScoreResult, Long> {

    boolean existsByApplicationId(Long applicationId);

    /** Latest result for a user — ordered by evaluatedAt desc, take first. */
    @Query("SELECT r FROM CreditScoreResult r WHERE r.userId = :userId ORDER BY r.evaluatedAt DESC")
    List<CreditScoreResult> findByUserIdOrderByEvaluatedAtDesc(@Param("userId") Long userId);
}

