package com.example.demo.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
        

        // ── 404 ────────────────────────────────────────────────────────────────────

        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex,
                        HttpServletRequest request) {
                log.warn("Resource not found: {}", ex.getMessage());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                                ApiError.builder()
                                                .status(HttpStatus.NOT_FOUND.value())
                                                .error("Not Found")
                                                .message(ex.getMessage())
                                                .path(request.getRequestURI())
                                                .build());
        }

        // ── 400 ────────────────────────────────────────────────────────────────────

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex,
                        HttpServletRequest request) {
                List<String> details = ex.getBindingResult().getFieldErrors().stream()
                                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                                .toList();

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                                ApiError.builder()
                                                .status(HttpStatus.BAD_REQUEST.value())
                                                .error("Validation Failed")
                                                .message("One or more fields failed validation")
                                                .path(request.getRequestURI())
                                                .details(details)
                                                .build());
        }

        // ── 403 ────────────────────────────────────────────────────────────────────

        /**
         * Thrown by Spring Security when an authenticated user lacks the required role.
         * Must be declared here — if left to the catch-all it would return 500.
         * Note: 401 (unauthenticated) is handled at the filter level, not here.
         */
        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex,
                        HttpServletRequest request) {
                log.warn("Access denied at {}: {}", request.getRequestURI(), ex.getMessage());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                                ApiError.builder()
                                                .status(HttpStatus.FORBIDDEN.value())
                                                .error("Forbidden")
                                                .message("You do not have permission to access this resource")
                                                .path(request.getRequestURI())
                                                .build());
        }

        // ── 401 ────────────────────────────────────────────────────────────────────

        @ExceptionHandler(BadCredentialsException.class)
        public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex,
                        HttpServletRequest request) {
                log.warn("Bad credentials at {}", request.getRequestURI());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                                ApiError.builder()
                                                .status(HttpStatus.UNAUTHORIZED.value())
                                                .error("Unauthorized")
                                                .message("Invalid username or password")
                                                .path(request.getRequestURI())
                                                .build());
        }

        // ── 409 ────────────────────────────────────────────────────────────────────

        @ExceptionHandler(DuplicateResourceException.class)
        public ResponseEntity<ApiError> handleDuplicate(DuplicateResourceException ex,
                        HttpServletRequest request) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                                ApiError.builder()
                                                .status(HttpStatus.CONFLICT.value())
                                                .error("Conflict")
                                                .message(ex.getMessage())
                                                .path(request.getRequestURI())
                                                .build());
        }

        // ── 400 (illegal argument) ──────────────────────────────────────────────────

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex,
                        HttpServletRequest request) {
                log.warn("Illegal argument at {}: {}", request.getRequestURI(), ex.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                                ApiError.builder()
                                                .status(HttpStatus.BAD_REQUEST.value())
                                                .error("Bad Request")
                                                .message(ex.getMessage())
                                                .path(request.getRequestURI())
                                                .build());
        }

        // ── 403 (bootstrap disabled) ────────────────────────────────────────────────

        @ExceptionHandler(IllegalStateException.class)
        public ResponseEntity<ApiError> handleIllegalState(IllegalStateException ex,
                        HttpServletRequest request) {
                log.warn("Illegal state at {}: {}", request.getRequestURI(), ex.getMessage());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                                ApiError.builder()
                                                .status(HttpStatus.FORBIDDEN.value())
                                                .error("Forbidden")
                                                .message(ex.getMessage())
                                                .path(request.getRequestURI())
                                                .build());
        }

        // ── 500 ────────────────────────────────────────────────────────────────────

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest request) {
                log.error("Unhandled exception at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                                ApiError.builder()
                                                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                                .error("Internal Server Error")
                                                .message("An unexpected error occurred")
                                                .path(request.getRequestURI())
                                                .build());
        }

}
