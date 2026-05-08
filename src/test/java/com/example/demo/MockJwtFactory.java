package com.example.demo;

import com.example.demo.security.JwtService;

public final class MockJwtFactory {

    static final String SECRET = "test-secret-key-must-be-at-least-32-chars";
    static final long EXPIRATION_MS = 3_600_000L;

    private static final JwtService JWT_SERVICE = new JwtService(SECRET, EXPIRATION_MS);

    private MockJwtFactory() {}

    public static String tokenFor(String username) {
        return JWT_SERVICE.generateToken(username);
    }

    public static String bearerTokenFor(String username) {
        return "Bearer " + tokenFor(username);
    }

    public static String expiredToken() {
        JwtService shortLived = new JwtService(SECRET, -1L);
        return shortLived.generateToken("expired-user");
    }

    public static String tokenSignedWithWrongSecret() {
        JwtService wrongKey = new JwtService("wrong-secret-key-must-be-at-least-32c", EXPIRATION_MS);
        return wrongKey.generateToken("hacker");
    }
}
