package com.salob.user_service.features.auth.dto;

public record RegisterRequest(
        String email,
        String username,
        String password
) {}
