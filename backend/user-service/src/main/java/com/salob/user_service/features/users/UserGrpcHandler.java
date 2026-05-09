package com.salob.user_service.features.users;

import com.salob.proto.user.UserIDsRequest;
import com.salob.proto.user.UserIDsResponse;
import com.salob.proto.user.UserServiceGrpc;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.server.service.GrpcService;

import java.util.List;
import java.util.UUID;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class UserGrpcHandler extends UserServiceGrpc.UserServiceImplBase {
    private final UserService userService;

    @Override
    public void getAllUserIDs(UserIDsRequest request, StreamObserver<UserIDsResponse> responseObserver) {
        try {
            List<String> userIDs = userService.getAllUserIDs().stream().map(UUID::toString).toList();
            UserIDsResponse response = UserIDsResponse.newBuilder().addAllIds(userIDs).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error fetching user IDs: {}", e.getMessage());
            responseObserver.onError(e);
        }
    }

    @PostConstruct
    public void init() {
        System.out.println("!!!!! GRPC BEAN ALIVE !!!!!");
    }
}
