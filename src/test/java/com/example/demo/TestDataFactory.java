package com.example.demo;

import com.example.demo.audit.AuditLog;

import java.time.LocalDateTime;

public final class TestDataFactory {

    private TestDataFactory() {}

    public static AuditLog auditLog() {
        return AuditLog.builder()
                .userId(1L)
                .endpoint("/api/v1/users")
                .httpMethod("GET")
                .statusCode(200)
                .ipAddress("127.0.0.1")
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static AuditLog auditLog(String endpoint, String method, int status) {
        return AuditLog.builder()
                .endpoint(endpoint)
                .httpMethod(method)
                .statusCode(status)
                .ipAddress("127.0.0.1")
                .createdAt(LocalDateTime.now())
                .build();
    }
}
