package com.salob.ai_service.api.chat;

import com.salob.ai_service.api.chat.tools.eatery.EateryTools;
import com.salob.ai_service.api.chat.tools.food_entry.FoodEntryTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatConfig {

    @Bean
    public ChatClient chatClient(
            DeepSeekChatModel chatModel,
            FoodEntryTools foodPriceTool,
            EateryTools eateryTools
    ) {
        return ChatClient.builder(chatModel)
                .defaultTools(foodPriceTool, eateryTools)  // Register the tool
                .build();
    }
}
