package com.example.demo.exception;

/**
 * Thrown when a credit score evaluation is requested for an application_id
 * that already has a result. Maps to HTTP 409.
 */
public class DuplicateEvaluationException extends RuntimeException {

    public DuplicateEvaluationException(Long applicationId) {
        super("Credit score already evaluated for applicationId: " + applicationId);
    }
}
