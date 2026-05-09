package com.example.demo.exception;

/**
 * Thrown by InternalKeyInterceptor when X-Internal-Key header is missing or invalid.
 * Maps to HTTP 403.
 */
public class InvalidInternalKeyException extends RuntimeException {

    public InvalidInternalKeyException() {
        super("Missing or invalid X-Internal-Key header");
    }
}
