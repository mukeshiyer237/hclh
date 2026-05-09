-- V4: User lifecycle events table.
-- Stores domain events consumed from Kafka users-topic.
-- Separate from audit_log which tracks HTTP requests.
-- event_type: USER_CREATED | USER_DELETED
-- payload: raw JSON of the event as received from Kafka

CREATE TABLE IF NOT EXISTS user_lifecycle_events (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT        NOT NULL,
    username    VARCHAR(100)  NOT NULL,
    event_type  VARCHAR(50)   NOT NULL,
    payload     TEXT          NOT NULL,
    occurred_at TIMESTAMP     NOT NULL,
    recorded_at TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ule_user_id    ON user_lifecycle_events (user_id);
CREATE INDEX IF NOT EXISTS idx_ule_event_type ON user_lifecycle_events (event_type);
