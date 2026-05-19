package com.salob.user_service.api.auth.dto;

public record LoginRequest(
        String usernameOrEmail,
        String password
) {}
