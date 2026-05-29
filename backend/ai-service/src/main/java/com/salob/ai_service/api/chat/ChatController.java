package com.salob.ai_service.api.chat;

import com.salob.ai_service.api.chat.dto.ChatStreamRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {
	private final ChatService chatService;

	@PostMapping(
			value = "/stream",
			consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.TEXT_EVENT_STREAM_VALUE
	)
	public Flux<ServerSentEvent<String>> streamChat(@Valid @RequestBody ChatStreamRequest request) {
		return chatService.streamChat(request);
	}
}
