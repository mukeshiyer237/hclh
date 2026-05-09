package com.example.demo.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record ChangeRoleRequest(
        @NotBlank(message = "Role must not be blank")
        String role
) {}