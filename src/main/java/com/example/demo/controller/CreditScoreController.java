package com.example.demo.controller;

import com.example.demo.domain.dto.CreditScoreRequest;
import com.example.demo.domain.dto.CreditScoreResponse;
import com.example.demo.domain.dto.CreditScoreRuleDTO;
import com.example.demo.domain.dto.RuleCacheRefreshResponse;
import com.example.demo.service.CreditScoreQueryService;
import com.example.demo.service.CreditScoringService;
import com.example.demo.service.RuleCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Internal-only credit scoring endpoints.
 * All routes are gated by InternalKeyInterceptor (X-Internal-Key header).
 * Zero business logic here — delegate immediately to service layer.
 */
@Tag(name = "Credit Scoring", description = "Internal endpoints for credit score evaluation and retrieval")
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class CreditScoreController {

    private final CreditScoringService creditScoringService;
    private final CreditScoreQueryService creditScoreQueryService;
    private final RuleCacheService ruleCacheService;

    // ── POST /internal/credit-scores ────────────────────────────────────────────

    @Operation(
            summary     = "Evaluate and store a credit score",
            description = "Runs the rule engine against the supplied financial profile, " +
                          "persists the result, and publishes a credit.score.evaluated event. " +
                          "Idempotent — returns 409 if applicationId was already evaluated."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Score evaluated and stored",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CreditScoreResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failure on request body", content = @Content),
            @ApiResponse(responseCode = "403", description = "Missing or invalid X-Internal-Key",  content = @Content),
            @ApiResponse(responseCode = "409", description = "applicationId already evaluated",    content = @Content),
            @ApiResponse(responseCode = "500", description = "Rule engine found no matching rule", content = @Content)
    })
    @PostMapping("/credit-scores")
    public ResponseEntity<CreditScoreResponse> evaluate(
            @Valid @RequestBody CreditScoreRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(creditScoringService.evaluate(request));
    }

    // ── GET /internal/credit-scores/{userId} ────────────────────────────────────

    @Operation(
            summary     = "Get latest credit score for a user",
            description = "Returns the most recent evaluated result for the given userId."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Latest score returned",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CreditScoreResponse.class))),
            @ApiResponse(responseCode = "403", description = "Missing or invalid X-Internal-Key", content = @Content),
            @ApiResponse(responseCode = "404", description = "No score found for userId",         content = @Content)
    })
    @GetMapping("/credit-scores/{userId}")
    public ResponseEntity<CreditScoreResponse> getLatest(
            @Parameter(description = "ID of the user", required = true)
            @PathVariable Long userId) {
        return ResponseEntity.ok(creditScoreQueryService.getLatestByUserId(userId));
    }

    // ── GET /internal/credit-scores/{userId}/history ────────────────────────────

    @Operation(
            summary     = "Get full scoring history for a user",
            description = "Returns all evaluated results for the given userId, newest first."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "History returned",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = CreditScoreResponse.class)))),
            @ApiResponse(responseCode = "403", description = "Missing or invalid X-Internal-Key", content = @Content),
            @ApiResponse(responseCode = "404", description = "No scores found for userId",        content = @Content)
    })
    @GetMapping("/credit-scores/{userId}/history")
    public ResponseEntity<List<CreditScoreResponse>> getHistory(
            @Parameter(description = "ID of the user", required = true)
            @PathVariable Long userId) {
        return ResponseEntity.ok(creditScoreQueryService.getHistoryByUserId(userId));
    }

    // ── GET /internal/credit-score-rules ────────────────────────────────────────

    @Operation(
            summary     = "List all credit score rules",
            description = "Returns every rule row (active and inactive). " +
                          "Use this to inspect the current rule config without a DB client."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rules returned",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = CreditScoreRuleDTO.class)))),
            @ApiResponse(responseCode = "403", description = "Missing or invalid X-Internal-Key", content = @Content)
    })
    @GetMapping("/credit-score-rules")
    public ResponseEntity<List<CreditScoreRuleDTO>> listRules() {
        return ResponseEntity.ok(ruleCacheService.getAllRules());
    }

    // ── PUT /internal/credit-score-rules/refresh ────────────────────────────────

    @Operation(
            summary     = "Flush and reload the rule cache",
            description = "Forces an immediate reload of active rules from the database. " +
                          "Call this after inserting or toggling a rule row — no deployment needed."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cache refreshed",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RuleCacheRefreshResponse.class))),
            @ApiResponse(responseCode = "403", description = "Missing or invalid X-Internal-Key", content = @Content)
    })
    @PutMapping("/credit-score-rules/refresh")
    public ResponseEntity<RuleCacheRefreshResponse> refreshCache() {
        int count = ruleCacheService.refresh();
        return ResponseEntity.ok(new RuleCacheRefreshResponse(count, LocalDateTime.now()));
    }
}
