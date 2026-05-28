package com.salob.ai_service.api.chat;

import com.salob.ai_service.api.chat.dto.ChatRequest;
import com.salob.ai_service.api.chat.dto.ChatResponse;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/chat")
public class ChatController {


    @GetMapping("/stream")
    public Flux<ChatResponse> handleStreamChatRequest(@RequestBody ChatRequest chatRequest) {
        var chatResponse = new ChatResponse(
                "Received your message: " + chatRequest.content() +
                        "Processing your request... Here is the response to your message: " +
                        "[Simulated AI Response] Thank you for chatting with us!"
        );
        return Flux.just(chatResponse);
    }
}
