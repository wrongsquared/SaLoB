package com.salob.ai_service.api.chat.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ChatStreamRequest(
		@Size(max = 100)
		String conversationId,

		@NotBlank
		@Size(max = 4_000)
		String message,

		@Valid
		@Size(max = 20)
		List<ChatMessageDto> history) {
}
