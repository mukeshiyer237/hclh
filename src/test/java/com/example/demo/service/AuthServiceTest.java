package com.example.demo.service;

import com.example.demo.controller.dto.AuthResponse;
import com.example.demo.controller.dto.LoginRequest;
import com.example.demo.controller.dto.RegisterRequest;
import com.example.demo.domain.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtService;
import com.example.demo.security.SecurityUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository       userRepository;
    @Mock PasswordEncoder      passwordEncoder;
    @Mock JwtService           jwtService;
    @Mock AuthenticationManager authenticationManager;

    @InjectMocks AuthService authService;

    private RegisterRequest registerRequest;
    private User            savedUser;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest("alice", "alice@example.com", "Password1!");
        savedUser = User.builder()
                .username("alice")
                .email("alice@example.com")
                .password("encoded")
                .role("USER")
                .build();
    }

    // ── register ─────────────────────────────────────────────────────────────

    @Test
    void register_happyPath_returnsTokenAndUsername() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password1!")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken("alice")).thenReturn("jwt-token");

        AuthResponse response = authService.register(registerRequest);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.username()).isEqualTo("alice");
        assertThat(response.role()).isEqualTo("USER");

        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode("Password1!");
    }

    @Test
    void register_duplicateUsername_throwsIllegalArgument() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("alice");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_duplicateEmail_throwsIllegalArgument() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("alice@example.com");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_passwordIsEncoded_notStoredInPlainText() {
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("Password1!")).thenReturn("$2a$bcrypt");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken(anyString())).thenReturn("token");

        authService.register(registerRequest);

        verify(passwordEncoder).encode("Password1!");
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    void login_validCredentials_returnsToken() {
        SecurityUser principal = new SecurityUser(savedUser);
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtService.generateToken("alice")).thenReturn("login-token");

        AuthResponse response = authService.login(new LoginRequest("alice", "Password1!"));

        assertThat(response.token()).isEqualTo("login-token");
        assertThat(response.username()).isEqualTo("alice");
        // SecurityUser prefixes role with "ROLE_" per Spring Security convention
        assertThat(response.role()).isEqualTo("ROLE_USER");
    }

    @Test
    void login_badCredentials_propagatesException() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
    }
}
