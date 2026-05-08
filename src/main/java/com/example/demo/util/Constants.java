package com.example.demo.util;

public final class Constants {

    private Constants() {}

    // API
    public static final String API_PREFIX = "/api/v1";

    // JWT
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    // Roles
    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    // Kafka Topics
    public static final String TOPIC_DEMO = "demo-topic";

    // Audit Actions
    public static final String ACTION_CREATE = "CREATE";
    public static final String ACTION_UPDATE = "UPDATE";
    public static final String ACTION_DELETE = "DELETE";
}
