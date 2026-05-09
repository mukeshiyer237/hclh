package com.example.demo.controller.dto;
import java.time.LocalDateTime;

public record UserResponse(
    Long id,
    String username,
    String email,
    String role,
    LocalDateTime createdAt,
    LocalDateTime deletedAt
 ) {}





