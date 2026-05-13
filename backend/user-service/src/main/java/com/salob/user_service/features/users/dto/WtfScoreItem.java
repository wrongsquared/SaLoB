package com.salob.user_service.features.users.dto;

import java.util.UUID;

public record WtfScoreItem(
        UUID userId,
        double wtfScore
) {}
