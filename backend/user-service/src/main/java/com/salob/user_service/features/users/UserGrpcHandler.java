package com.salob.user_service.features.users;

import com.salob.proto.user.*;
import com.salob.user_service.features.User;
import com.salob.user_service.storage.minio.MinioStorageService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class UserGrpcHandler extends UserServiceGrpc.UserServiceImplBase {
    private final UserService userService;
    private final MinioStorageService minioStorageService;

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

    @Override
    public void getUserWtfScore(UserWtfRequest request, StreamObserver<UserWtfResponse> responseObserver) {
        try {
            UUID userId = UUID.fromString(request.getUserId());
            double wtfScore = userService.getUserWtfScore(userId);
            UserWtfResponse response = UserWtfResponse.newBuilder().setWtfScore(wtfScore).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error fetching WTF score for user: {}", e.getMessage());
            responseObserver.onError(e);
        }
    }

    @Override
    public void getUserWtfScoreBatch(UserWtfBatchRequest request, StreamObserver<UserWtfBatchResponse> responseObserver) {
        try {
            List<UUID> userIds = request.getUserIdsList().stream()
                    .map(UUID::fromString)
                    .toList();

            List<UserWtfBatchResponseItem> wtfBatchResponseItems = userService.getUserWtfScoreBatch(userIds)
                    .stream()
                    .map(item ->
                            UserWtfBatchResponseItem.newBuilder()
                                    .setUserId(item.userId().toString())
                                    .setWtfScore(item.wtfScore())
                                    .build()
                    ).toList();

            UserWtfBatchResponse res = UserWtfBatchResponse.newBuilder().addAllItems(wtfBatchResponseItems).build();
            responseObserver.onNext(res);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error fetching batch WTF scores: {}", e.getMessage());
            responseObserver.onError(e);
        }
    }

    @Override
    public void getUserDetails(UserDetailsRequest request, StreamObserver<UserDetailsResponse> responseObserver) {
        try {
            UUID userId = UUID.fromString(request.getUserId());
            User user = userService.findById(userId);
            String photoUrl = minioStorageService.getPresignedUrl(user.getAvatarObjKey(), Duration.ofMinutes(30));
            long tenureDays = (Duration.between(user.getCreatedAt(), Instant.now())).toDays();

            UserDetailsResponse response = UserDetailsResponse.newBuilder()
                    .setUserId(request.getUserId())
                    .setUsername(user.getUsername())
                    .setPhotoUrl(photoUrl)
                    .setWtfScore(user.getWtfScore())
                    .setTenureDays((int)tenureDays)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error fetching user details: {}", e.getMessage());
            responseObserver.onError(e);
        }
    }

    //    @PostConstruct
//    public void init() {
//        System.out.println("!!!!! GRPC BEAN ALIVE !!!!!");
//    }
}
