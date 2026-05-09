package com.example.demo.kafka.event;

import java.time.LocalDateTime;

public record UserDeletedEvent(
        Long userId,
        String username,
        LocalDateTime deletedAt
) {}