package com.salob.ai_service.api.chat.tools.food_entry;

import com.salob.proto.foodEntry.ConsensusFoodEntryRequest;
import com.salob.proto.foodEntry.ConsensusFoodEntryResponse;
import com.salob.proto.foodEntry.FoodEntryServiceGrpc;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FoodEntryTools {
    @GrpcClient("food-service")
    private FoodEntryServiceGrpc.FoodEntryServiceBlockingStub foodEntryServiceStub;

    @Tool(description = "Get the current price of a food item from a specific eatery")
    public Integer getFoodPrice(
            @ToolParam(description = "The name of the food item")
            String foodName,

            @ToolParam(description = "The name of the eatery")
            String eateryName
    ) {
        try {
            log.info("Getting price for food item '{}' from eatery '{}'", foodName, eateryName);

            var req = ConsensusFoodEntryRequest.newBuilder()
                    .setFoodName(foodName)
                    .setEateryName(eateryName)
                    .build();
            ConsensusFoodEntryResponse res = foodEntryServiceStub.getConsensusFoodEntry(req);
            return res.getSgCents();
        } catch (Exception e) {
            log.warn("Tool failed, {}", e.getMessage());
            return 0;
        }
    }
}
