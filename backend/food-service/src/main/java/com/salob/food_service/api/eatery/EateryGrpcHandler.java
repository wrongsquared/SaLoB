package com.salob.food_service.api.eatery;

import com.salob.food_service.api._domain.Eatery;
import com.salob.food_service.api._exceptions.EateryNotFoundException;
import com.salob.food_service.api.eatery.dto.EateryPreviewDTO;
import com.salob.food_service.api.onemap.OneMapClient;
import com.salob.proto.eatery.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;
import java.util.UUID;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class EateryGrpcHandler extends EateryServiceGrpc.EateryServiceImplBase {
	private final EateryService eateryService;
	private final OneMapClient oneMapClient;

	@Override
	public void getEatery(EateryRequest request, StreamObserver<EateryResponse> responseObserver) {
		try {
			UUID eateryId = UUID.fromString(request.getEateryId());
			Eatery eatery = eateryService.findById(eateryId);
			var response = EateryResponse.newBuilder().setId(eatery.getId().toString()).setName(eatery.getName())
					.setLat(eatery.getLocation().getY()).setLon(eatery.getLocation().getX()).build();

			responseObserver.onNext(response);
			responseObserver.onCompleted();
		} catch (EateryNotFoundException e) {
			responseObserver.onError(e);
		}
	}

	@Override
	public void getNearbyEateries(NearbyEateriesRequest request,
			StreamObserver<NearbyEateriesResponse> responseObserver) {
		int TOP_K = 7;
		double RADIUS_METERS = 1000;

		try {
			String locationToSearch = request.getLocation();
			log.info("Received request to find nearby eateries for location: {}", locationToSearch);
			double[] coords = oneMapClient.geocode(locationToSearch);
			log.info("Found the coords for location {}, found {} {}", locationToSearch, coords[0], coords[1]);

			List<EateryPreviewDTO> nearbyEateriesFound = eateryService.findNearestKEateriesWithinRadius(coords[0],
					coords[1], RADIUS_METERS, TOP_K);
			log.info("Found {} eateries near location {}", nearbyEateriesFound.size(), locationToSearch);
			if (nearbyEateriesFound.isEmpty()) {
				responseObserver.onError(Status.NOT_FOUND
						.withDescription("No eateries found near location: " + locationToSearch).asRuntimeException());
				return;
			}

			List<NearbyEatery> nearbyEateriesResponse = nearbyEateriesFound.stream().map(e -> NearbyEatery.newBuilder()
					.setId(e.eateryId().toString()).setName(e.name()).setAddress(e.address()).build()).toList();

			var response = NearbyEateriesResponse.newBuilder().addAllEateries(nearbyEateriesResponse).build();
			responseObserver.onNext(response);
			responseObserver.onCompleted();
		} catch (Exception e) {
			responseObserver.onError(Status.INTERNAL.withCause(e).asRuntimeException());
		}
	}

	// DEBUG
	// @PostConstruct
	// public void init() {
	// System.out.println("!!!!! GRPC BEAN ALIVE !!!!!");
	// }
}
