package com.example.demo.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByUserId(Long userId);

    List<AuditLog> findByEndpoint(String endpoint);

    List<AuditLog> findByHttpMethodAndStatusCode(String httpMethod, Integer statusCode);
}
