-- V6: Credit score results table (immutable evaluation records)
CREATE TABLE credit_score_results (
    result_id               BIGSERIAL       NOT NULL,
    application_id          BIGINT          NOT NULL,
    user_id                 BIGINT          NOT NULL,
    application_ref         VARCHAR(100)    NOT NULL,
    final_score             INT             NOT NULL,
    score_basis             VARCHAR(20)     NOT NULL DEFAULT 'CALCULATED'
                                                CHECK (score_basis IN ('CALCULATED')),
    annual_salary_snapshot  DECIMAL(15,2)   NOT NULL,
    cards_count_snapshot    SMALLINT        NOT NULL,
    evaluated_at            TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_credit_score_results PRIMARY KEY (result_id),
    CONSTRAINT uq_application_eval     UNIQUE (application_id)
);

CREATE INDEX idx_csr_user_id         ON credit_score_results (user_id);
CREATE INDEX idx_csr_application_ref ON credit_score_results (application_ref);
CREATE INDEX idx_csr_evaluated_at    ON credit_score_results (evaluated_at);
