package com.salob.user_service.features.auth.dto;

public record LoginRequest(
        String usernameOrEmail,
        String password
) {}

