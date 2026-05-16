package com.salob.user_service.api.auth.dto;

public record RegisterRequest(
        String email,
        String username,
        String password
) {}
