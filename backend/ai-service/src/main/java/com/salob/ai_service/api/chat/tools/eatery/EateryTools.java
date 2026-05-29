package com.salob.ai_service.api.chat.tools.eatery;

import com.salob.proto.eatery.EateryServiceGrpc;
import com.salob.proto.eatery.NearbyEateriesRequest;
import com.salob.proto.eatery.NearbyEateriesResponse;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class EateryTools {
    @GrpcClient("food-service")
    private EateryServiceGrpc.EateryServiceBlockingStub eateryServiceStub;

    @Tool(description = "Get a list of eateries near a given location")
    public List<EateryResponse> getEateriesNear(
            @ToolParam(description = "The location to search around, e.g. 'Orchard Road', 'Tiong Bahru'")
            String location
    ) {
        try {
            log.info("Fetching eateries near '{}'", location);

            var req = NearbyEateriesRequest.newBuilder().setLocation(location).build();
            NearbyEateriesResponse res = eateryServiceStub.getNearbyEateries(req);

            return res.getEateriesList().stream()
                    .map(e -> new EateryResponse(UUID.fromString(e.getId()), e.getName(), e.getAddress())).toList();
        } catch (Exception e) {
            log.warn("Tool failed, {}", e.getMessage());
            return List.of();
        }
    }
}
