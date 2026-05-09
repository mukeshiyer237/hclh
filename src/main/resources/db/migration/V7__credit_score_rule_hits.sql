-- V7: Credit score rule hits table (per-evaluation audit trail of fired rules)
CREATE TABLE credit_score_rule_hits (
    hit_id          BIGSERIAL   NOT NULL,
    result_id       BIGINT      NOT NULL,
    rule_id         BIGINT      NOT NULL,
    points_awarded  INT         NOT NULL,
    created_at      TIMESTAMP   NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_hits       PRIMARY KEY (hit_id),
    CONSTRAINT fk_hit_result FOREIGN KEY (result_id)
                                 REFERENCES credit_score_results(result_id)
                                 ON DELETE CASCADE,
    CONSTRAINT fk_hit_rule   FOREIGN KEY (rule_id)
                                 REFERENCES credit_score_rules(rule_id)
                                 ON DELETE RESTRICT
);

CREATE INDEX idx_hit_result ON credit_score_rule_hits (result_id);
CREATE INDEX idx_hit_rule   ON credit_score_rule_hits (rule_id);
