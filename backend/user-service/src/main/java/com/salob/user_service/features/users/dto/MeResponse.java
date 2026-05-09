package com.salob.user_service.features.users.dto;

import com.salob.user_service.features.users.UserRole;
import lombok.Builder;

import java.util.UUID;

//public record MeResponse(
//        UUID id,
//        String email,
//        String username,
//        UserRole role,
//        String avatarUrl,
//        double wtfScore,
//        int totalSubmissions,
//        int upvotesReceived,
//        int downvotesReceived,
//        int anomaliesFlagged,
//        Instant createdAt,
//        boolean isActive
//) {}

@Builder
public record MeResponse(
        UUID id,
        String email,
        String username,
        UserRole role,
        String avatarUrl
) {}
