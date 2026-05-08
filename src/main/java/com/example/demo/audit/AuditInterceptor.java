package com.example.demo.audit;

import com.example.demo.security.SecurityUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Fires after every matched request completes.
 * Writes one row to audit_log: who, what endpoint, what method, what status, from where.
 *
 * Registered in WebMvcConfig on /api/** (excluding auth and Swagger paths).
 * Failures are swallowed with an error log — audit must never break the response.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditInterceptor implements HandlerInterceptor {

    private final AuditLogRepository auditLogRepository;

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        try {
            AuditLog entry = AuditLog.builder()
                    .userId(resolveUserId())
                    .endpoint(request.getRequestURI())
                    .httpMethod(request.getMethod())
                    .statusCode(response.getStatus())
                    .ipAddress(resolveIpAddress(request))
                    .build();

            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.error("Audit write failed for [{} {}]: {}",
                    request.getMethod(), request.getRequestURI(), e.getMessage(), e);
        }
    }

    /**
     * Returns the authenticated user's DB id.
     * Returns null for anonymous / unauthenticated requests (stored as NULL in audit_log).
     */
    private Long resolveUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        if (auth.getPrincipal() instanceof SecurityUser securityUser) {
            return securityUser.getId();
        }
        return null;
    }

    /**
     * Respects X-Forwarded-For so the real client IP is captured behind a proxy/load balancer.
     * Takes the first IP in the chain (closest to the client).
     */
    private String resolveIpAddress(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
