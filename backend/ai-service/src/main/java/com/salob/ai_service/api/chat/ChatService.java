package com.salob.ai_service.api.chat;

import com.salob.ai_service.api.chat.dto.ChatMessageDto;
import com.salob.ai_service.api.chat.dto.ChatStreamRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {
    private static final int MAX_TOTAL_CONTEXT_CHARS = 12_000; // Total context, NOT counting system prompt or latest user message
    private static final String DONE_PAYLOAD = "[DONE]";
    private static final String SYSTEM_PROMPT = """
		You are 'AskCox', an AI assistant for the SG Crowdsourced Price Intelligence platform known as 'SaLoB'.
		Use tools to answer questions about prices, eateries, and available foods.

		Never invent prices or availability. If required info is missing (food or eatery), ask a clarifying question.
		If a tool returns no match or multiple candidates, suggest options and ask the user to clarify.

		For navigation requests, return a concise answer that clearly states the destination or action.
		Keep responses concise and friendly.
	""";

    private final ChatClient chatClient;

    public Flux<ServerSentEvent<String>> streamChat(ChatStreamRequest request) {
        List<ChatMessageDto> history = getTruncatedHistory(request.history(), MAX_TOTAL_CONTEXT_CHARS);

        List<Message> promptFriendlyHistory = new ArrayList<>();
        promptFriendlyHistory.add(new SystemMessage(SYSTEM_PROMPT));
        promptFriendlyHistory.addAll(convertToPromptFriendlyFormat(history));
        promptFriendlyHistory.add(new UserMessage(request.message().trim()));

        return chatClient
                .prompt(new Prompt(promptFriendlyHistory))
                .stream()
                .content()
                .filter(text -> !text.isBlank())
                .map(this::chatEvent)
                .concatWith(Flux.just(doneEvent()))
                .onErrorResume(e -> {
                    log.error("Stream error", e);
                    return Flux.just(errorEvent("Stream error: " + e.getMessage()), doneEvent());
                });
    }

    private List<ChatMessageDto> getTruncatedHistory(List<ChatMessageDto> history, int maxTotalContextChars) {
        List<ChatMessageDto> effectiveHistory = new ArrayList<>(history == null ? List.of() : history);
        Collections.reverse(effectiveHistory); // When iterating, newest first

        List<ChatMessageDto> truncatedHistory = new ArrayList<>();

        int remainingContextChars = maxTotalContextChars;
        for (ChatMessageDto message : effectiveHistory) {
            if (message == null || message.content() == null || message.content().isBlank()) {
                continue;
            }

            String content = message.content().trim();
            if (remainingContextChars <= 0) {
                break;
            }

            if (content.length() <= remainingContextChars) {
                remainingContextChars -= content.length();
                truncatedHistory.add(new ChatMessageDto(message.role(), content));
            } else {
                truncatedHistory.add(new ChatMessageDto(
                        message.role(),
                        content.substring(0, remainingContextChars)
                ));
                break;
            }
        }
        return truncatedHistory;
    }

    private List<Message> convertToPromptFriendlyFormat(List<ChatMessageDto> history) {
        List<Message> messagesForPrompt = new ArrayList<>();
        for (ChatMessageDto msg : history) {
            messagesForPrompt.add(toPromptFriendlyMessage(msg));
        }
        return messagesForPrompt;
    }

    private Message toPromptFriendlyMessage(ChatMessageDto messageDto) {
        String role = messageDto.role().trim().toLowerCase(Locale.ROOT);
        String content = messageDto.content().trim();

        return switch (role) {
            case "user" -> new UserMessage(content);
            case "assistant" -> new AssistantMessage(content);
            default -> throw new IllegalArgumentException("Unsupported chat role: " + messageDto.role());
        };
    }

    private ServerSentEvent<String> chatEvent(String text) {
        return ServerSentEvent.builder(text).event("chat").build();
    }

    private ServerSentEvent<String> errorEvent(String message) {
        return ServerSentEvent.builder(message).event("error").build();
    }

    private ServerSentEvent<String> doneEvent() {
        return ServerSentEvent.builder(DONE_PAYLOAD).event("done").build();
    }
}
