package com.salob.user_service.api.users.dto;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record MeResponse(
        UUID id,
        String email,
        String username,
        List<String> roles,
        String avatarUrl
) {}
