-- V2: HTTP request audit log table
CREATE TABLE IF NOT EXISTS audit_log (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT,
    endpoint    VARCHAR(500) NOT NULL,
    http_method VARCHAR(10)  NOT NULL,
    status_code INTEGER      NOT NULL,
    ip_address  VARCHAR(45),
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_audit_log_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_audit_log_user_id    ON audit_log (user_id);
CREATE INDEX idx_audit_log_created_at ON audit_log (created_at DESC);
CREATE INDEX idx_audit_log_endpoint   ON audit_log (endpoint);
