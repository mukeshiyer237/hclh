package com.example.demo.controller;

import com.example.demo.controller.dto.AuthResponse;
import com.example.demo.controller.dto.LoginRequest;
import com.example.demo.controller.dto.RegisterRequest;
import com.example.demo.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock    AuthService    authService;
    @InjectMocks AuthController authController;

    private AuthResponse stubResponse;

    @BeforeEach
    void setUp() {
        stubResponse = new AuthResponse("jwt-token", "alice", "USER");
    }

    // ── register ──────────────────────────────────────────────────────────────

    @Test
    void register_returns201WithBody() {
        RegisterRequest req = new RegisterRequest("alice", "alice@example.com", "Password1!");
        when(authService.register(req)).thenReturn(stubResponse);

        ResponseEntity<AuthResponse> resp = authController.register(req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().token()).isEqualTo("jwt-token");
        assertThat(resp.getBody().username()).isEqualTo("alice");
        assertThat(resp.getBody().role()).isEqualTo("USER");

        verify(authService).register(req);
    }

    @Test
    void register_delegatesExceptionFromService() {
        RegisterRequest req = new RegisterRequest("alice", "alice@example.com", "Password1!");
        when(authService.register(req)).thenThrow(new IllegalArgumentException("Username already taken: alice"));

        assertThatThrownBy(() -> authController.register(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("alice");
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    void login_returns200WithBody() {
        LoginRequest req = new LoginRequest("alice", "Password1!");
        when(authService.login(req)).thenReturn(stubResponse);

        ResponseEntity<AuthResponse> resp = authController.login(req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().token()).isEqualTo("jwt-token");
        assertThat(resp.getBody().username()).isEqualTo("alice");

        verify(authService).login(req);
    }

    @Test
    void login_delegatesExceptionFromService() {
        LoginRequest req = new LoginRequest("alice", "wrong");
        when(authService.login(req))
                .thenThrow(new org.springframework.security.authentication.BadCredentialsException("bad"));

        assertThatThrownBy(() -> authController.login(req))
                .isInstanceOf(org.springframework.security.authentication.BadCredentialsException.class);
    }
}
