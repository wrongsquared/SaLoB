package com.salob.ai_service.api.chat.tools.eatery;

import java.util.UUID;

public record EateryResponse(
        UUID eateryId,
        String name,
        String address
) {}
