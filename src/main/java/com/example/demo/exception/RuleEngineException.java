package com.example.demo.exception;

/**
 * Thrown by RuleEngineService when no active rule matches the given inputs.
 * Indicates a gap in seed data — income ranges are not contiguous.
 * Maps to HTTP 500.
 */
public class RuleEngineException extends RuntimeException {

    public RuleEngineException(String message) {
        super(message);
    }
}
