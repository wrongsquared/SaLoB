package com.salob.food_service.features.eatery;

import com.salob.food_service.features.eatery.domain.Eatery;
import com.salob.food_service.features.eatery.exceptions.EateryNotFoundException;
import com.salob.proto.eatery.EateryRequest;
import com.salob.proto.eatery.EateryResponse;
import com.salob.proto.eatery.EateryServiceGrpc;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.UUID;

@GrpcService
@RequiredArgsConstructor
public class EateryGrpcHandler extends EateryServiceGrpc.EateryServiceImplBase {
    private final EateryService eateryService;

    @Override
    public void getEatery(EateryRequest request, StreamObserver<EateryResponse> responseObserver) {
        try {
            UUID eateryId = UUID.fromString(request.getEateryId());
            Eatery eatery = eateryService.findById(eateryId);
            var response = EateryResponse.newBuilder()
                    .setId(eatery.getId().toString())
                    .setName(eatery.getName())
                    .setLat(eatery.getLocation().getY())
                    .setLon(eatery.getLocation().getX())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (EateryNotFoundException e) {
            responseObserver.onError(e);
        }
    }

    // DEBUG
//    @PostConstruct
//    public void init() {
//        System.out.println("!!!!! GRPC BEAN ALIVE !!!!!");
//    }
}
