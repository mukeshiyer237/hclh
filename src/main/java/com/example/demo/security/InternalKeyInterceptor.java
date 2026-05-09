package com.example.demo.security;

import com.example.demo.exception.InvalidInternalKeyException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Guards all /internal/** routes.
 * Every request must carry an X-Internal-Key header whose value matches
 * the configured internal.api-key property.
 *
 * Throws InvalidInternalKeyException (→ 403) before the controller method runs.
 * Does NOT use Spring Security — this is a simple shared-secret gate for
 * service-to-service calls, not user authentication.
 */
@Slf4j
@Component
public class InternalKeyInterceptor implements HandlerInterceptor {

    static final String HEADER = "X-Internal-Key";

    private final String expectedKey;

    public InternalKeyInterceptor(@Value("${internal.api-key}") String expectedKey) {
        this.expectedKey = expectedKey;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        String provided = request.getHeader(HEADER);

        if (provided == null || !provided.equals(expectedKey)) {
            log.warn("Rejected /internal request — invalid or missing {}: uri={}",
                    HEADER, request.getRequestURI());
            throw new InvalidInternalKeyException();
        }

        return true;
    }
}
