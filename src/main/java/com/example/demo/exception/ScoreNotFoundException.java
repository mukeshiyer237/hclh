package com.example.demo.exception;

/**
 * Thrown when no credit score result exists for the requested userId.
 * Maps to HTTP 404.
 */
public class ScoreNotFoundException extends RuntimeException {

    public ScoreNotFoundException(Long userId) {
        super("No credit score found for userId: " + userId);
    }
}
