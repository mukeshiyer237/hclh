package com.example.demo.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standard error response body returned for every non-2xx response.
 * Shape: { timestamp, status, error, message, path, details? }
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL) // omit "details" when null
public class ApiError {

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime timestamp;

    private final int status;
    private final String error;
    private final String message;
    private final String path;

    /** Field-level validation errors — only present for 400 responses. */
    private final List<String> details;

    private ApiError(Builder builder) {
        this.timestamp = LocalDateTime.now();
        this.status    = builder.status;
        this.error     = builder.error;
        this.message   = builder.message;
        this.path      = builder.path;
        this.details   = builder.details;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private int status;
        private String error;
        private String message;
        private String path;
        private List<String> details;

        public Builder status(int status)           { this.status  = status;  return this; }
        public Builder error(String error)          { this.error   = error;   return this; }
        public Builder message(String message)      { this.message = message; return this; }
        public Builder path(String path)            { this.path    = path;    return this; }
        public Builder details(List<String> details){ this.details = details; return this; }

        public ApiError build() { return new ApiError(this); }
    }
}
