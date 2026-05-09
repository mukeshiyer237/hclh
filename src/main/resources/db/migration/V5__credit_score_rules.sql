-- V5: Credit score rules table (data-driven rule engine config)
CREATE TABLE credit_score_rules (
    rule_id         BIGSERIAL       NOT NULL,
    rule_code       VARCHAR(100)    NOT NULL,
    rule_name       VARCHAR(255)    NOT NULL,
    rule_category   VARCHAR(20)     NOT NULL CHECK (rule_category IN ('INCOME', 'CARDS_HELD')),
    min_value       DECIMAL(15,2)   NULL,
    max_value       DECIMAL(15,2)   NULL,
    score_points    INT             NOT NULL,
    priority_order  INT             NOT NULL DEFAULT 1,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    description     VARCHAR(500)    NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_credit_score_rules PRIMARY KEY (rule_id),
    CONSTRAINT uq_rule_code          UNIQUE (rule_code)
);

CREATE INDEX idx_active_priority ON credit_score_rules (is_active, priority_order);

INSERT INTO credit_score_rules
    (rule_code, rule_name, rule_category, min_value, max_value, score_points, priority_order, description)
VALUES
    ('CARDS_GTE_2',
     'Holds 2 or More Credit Cards',
     'CARDS_HELD',
     2, NULL, 300, 1,
     'Fires when numberOfCreditCards >= 2'),

    ('SALARY_GT_200K',
     'Annual Salary Greater Than 2,00,000',
     'INCOME',
     200000.01, NULL, 500, 2,
     'Fires when annualSalary > 200,000'),

    ('SALARY_50K_TO_200K',
     'Annual Salary Between 50,000-2,00,000',
     'INCOME',
     50000.00, 200000.00, 150, 3,
     'Fires when annualSalary >= 50,000 AND <= 2,00,000'),

    ('SALARY_LTE_50K',
     'Annual Salary 50,000 or Below',
     'INCOME',
     0, 49999.99, 50, 4,
     'Fires when annualSalary < 50,000');
