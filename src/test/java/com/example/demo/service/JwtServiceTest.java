package com.example.demo.service;

import com.example.demo.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-must-be-at-least-32-chars";
    private static final long EXPIRATION_MS = 3_600_000L;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, EXPIRATION_MS);
    }

    @Test
    void generateToken_returnsNonNullToken() {
        String token = jwtService.generateToken("alice");
        assertThat(token).isNotBlank();
    }

    @Test
    void extractUsername_returnsSubjectFromValidToken() {
        String token = jwtService.generateToken("alice");
        assertThat(jwtService.extractUsername(token)).isEqualTo("alice");
    }

    @Test
    void isValid_returnsTrueForFreshToken() {
        String token = jwtService.generateToken("alice");
        assertThat(jwtService.isValid(token)).isTrue();
    }

    @Test
    void isValid_returnsFalseForExpiredToken() {
        JwtService shortLived = new JwtService(SECRET, -1L);
        String token = shortLived.generateToken("alice");
        assertThat(jwtService.isValid(token)).isFalse();
    }

    @Test
    void extractUsername_returnsNullForTamperedToken() {
        String token = jwtService.generateToken("alice");
        String tampered = token.substring(0, token.length() - 4) + "XXXX";
        assertThat(jwtService.extractUsername(tampered)).isNull();
    }

    @Test
    void extractUsername_returnsNullForTokenSignedWithDifferentSecret() {
        JwtService other = new JwtService("other-secret-key-must-be-at-least-32c", EXPIRATION_MS);
        String token = other.generateToken("alice");
        assertThat(jwtService.extractUsername(token)).isNull();
    }

    @Test
    void extractUsername_returnsNullForGarbageInput() {
        assertThat(jwtService.extractUsername("not.a.token")).isNull();
        assertThat(jwtService.extractUsername("")).isNull();
        assertThat(jwtService.extractUsername(null)).isNull();
    }

    @Test
    void generateToken_differentUsersProduceDifferentTokens() {
        String t1 = jwtService.generateToken("alice");
        String t2 = jwtService.generateToken("bob");
        assertThat(t1).isNotEqualTo(t2);
    }
}
