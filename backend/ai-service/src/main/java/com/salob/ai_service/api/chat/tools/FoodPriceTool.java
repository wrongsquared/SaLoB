package com.salob.ai_service.api.chat.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

@Service
public class FoodPriceTool {

    @Tool(description = "Get the current price of a food item from a specific eatery")
    public String getFoodPrice(
            @ToolParam(description = "The name of the food item")
            String foodName,

            @ToolParam(description = "The name of the eatery")
            String eateryName
    ) {
        return "4.50";
    }
}
