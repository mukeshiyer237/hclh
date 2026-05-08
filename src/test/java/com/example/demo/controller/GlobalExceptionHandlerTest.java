package com.example.demo.controller;

import com.example.demo.exception.ApiError;
import com.example.demo.exception.GlobalExceptionHandler;
import com.example.demo.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/test");
    }

    @Test
    void resourceNotFound_returns404WithCorrectBody() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Widget", "id", 99);

        ResponseEntity<ApiError> response = handler.handleNotFound(ex, request);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getError()).isEqualTo("Not Found");
        assertThat(response.getBody().getMessage()).isEqualTo("Widget not found with id: '99'");
        assertThat(response.getBody().getPath()).isEqualTo("/api/v1/test");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    void resourceNotFound_detailsFieldIsNull() {
        ResponseEntity<ApiError> response = handler.handleNotFound(
                new ResourceNotFoundException("Thing"), request);
        assertThat(response.getBody().getDetails()).isNull();
    }

    @Test
    void validationFailure_returns400WithFieldDetails() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "body");
        bindingResult.addError(new FieldError("body", "name", "must not be blank"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ApiError> response = handler.handleValidation(ex, request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getDetails()).containsExactly("name: must not be blank");
    }

    @Test
    void validationFailure_multipleErrors_allDetailsIncluded() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "body");
        bindingResult.addError(new FieldError("body", "name", "must not be blank"));
        bindingResult.addError(new FieldError("body", "email", "must be a valid email"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ApiError> response = handler.handleValidation(ex, request);

        assertThat(response.getBody().getDetails()).hasSize(2);
    }

    @Test
    void accessDenied_returns403() {
        ResponseEntity<ApiError> response = handler.handleAccessDenied(
                new AccessDeniedException("forbidden"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(response.getBody().getError()).isEqualTo("Forbidden");
        assertThat(response.getBody().getDetails()).isNull();
    }

    @Test
    void genericException_returns500WithSafeMessage() {
        ResponseEntity<ApiError> response = handler.handleGeneric(
                new RuntimeException("internal details that must not leak"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
        assertThat(response.getBody().getMessage()).doesNotContain("internal details");
    }

    @Test
    void apiError_timestampIsAlwaysPopulated() {
        ResponseEntity<ApiError> response = handler.handleNotFound(
                new ResourceNotFoundException("X"), request);
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    void apiError_pathMatchesRequestUri() {
        request.setRequestURI("/api/v1/widgets/42");
        ResponseEntity<ApiError> response = handler.handleNotFound(
                new ResourceNotFoundException("Widget", "id", 42), request);
        assertThat(response.getBody().getPath()).isEqualTo("/api/v1/widgets/42");
    }
}
