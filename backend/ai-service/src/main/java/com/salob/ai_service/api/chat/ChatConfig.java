package com.salob.ai_service.api.chat;

import com.salob.ai_service.api.chat.tools.FoodPriceTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatConfig {

    @Bean
    public ChatClient chatClient(DeepSeekChatModel chatModel, FoodPriceTool foodPriceTool) {
        return ChatClient.builder(chatModel)
                .defaultTools(foodPriceTool)  // Register the tool
                .build();
    }
}
