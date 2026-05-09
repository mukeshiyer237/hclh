package com.example.demo.kafka.event;

import java.time.LocalDateTime;

public record UserCreatedEvent(
        Long userId,
        String username,
        String email,
        String role,
        LocalDateTime createdAt
) {}