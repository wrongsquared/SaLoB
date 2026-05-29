package com.salob.ai_service.api.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChatMessageDto(
		@NotBlank
		@Pattern(regexp = "(?i)^(user|assistant)$")
		String role,

		@NotBlank
		@Size(max = 4_000)
		String content) {
}
