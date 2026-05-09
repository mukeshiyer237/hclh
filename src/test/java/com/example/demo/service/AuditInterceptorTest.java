package com.example.demo.service;

import com.example.demo.audit.AuditInterceptor;
import com.example.demo.audit.AuditLog;
import com.example.demo.audit.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditInterceptorTest {

    @Mock AuditLogRepository auditLogRepository;
    @Mock HttpServletRequest request;
    @Mock HttpServletResponse response;

    @InjectMocks AuditInterceptor interceptor;

    @BeforeEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void afterCompletion_savesAuditRowWithRequestDetails() {
        when(request.getRequestURI()).thenReturn("/api/v1/users");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(response.getStatus()).thenReturn(200);

        interceptor.afterCompletion(request, response, new Object(), null);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();

        assertThat(saved.getEndpoint()).isEqualTo("/api/v1/users");
        assertThat(saved.getHttpMethod()).isEqualTo("GET");
        assertThat(saved.getStatusCode()).isEqualTo(200);
        assertThat(saved.getIpAddress()).isEqualTo("192.168.1.1");
        assertThat(saved.getUserId()).isNull();
    }

    @Test
    void afterCompletion_prefersXForwardedForOverRemoteAddr() {
        when(request.getRequestURI()).thenReturn("/api/v1/items");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1, 10.0.0.2");
        when(response.getStatus()).thenReturn(201);

        interceptor.afterCompletion(request, response, new Object(), null);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getIpAddress()).isEqualTo("10.0.0.1");
    }

    @Test
    void afterCompletion_storesNullUserIdForAnonymousUser() {
        when(request.getRequestURI()).thenReturn("/api/v1/public");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(response.getStatus()).thenReturn(200);

        interceptor.afterCompletion(request, response, new Object(), null);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isNull();
    }

    @Test
    void afterCompletion_doesNotPropagateRepositoryException() {
        when(request.getRequestURI()).thenReturn("/api/v1/users");
        when(request.getMethod()).thenReturn("DELETE");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(response.getStatus()).thenReturn(204);
        when(auditLogRepository.save(any())).thenThrow(new RuntimeException("DB down"));

        assertThatNoException().isThrownBy(
                () -> interceptor.afterCompletion(request, response, new Object(), null));
    }

    @Test
    void afterCompletion_capturesCorrectStatusCodeFor4xx() {
        when(request.getRequestURI()).thenReturn("/api/v1/missing");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(response.getStatus()).thenReturn(404);

        interceptor.afterCompletion(request, response, new Object(), null);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getStatusCode()).isEqualTo(404);
    }
}
