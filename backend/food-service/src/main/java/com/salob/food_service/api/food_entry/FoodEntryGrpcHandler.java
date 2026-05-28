package com.salob.food_service.api.food_entry;

import com.salob.food_service.api._domain.FoodEntry;
import com.salob.food_service.common.ConfidenceAlgorithm;
import com.salob.proto.foodEntry.ConsensusFoodEntryRequest;
import com.salob.proto.foodEntry.ConsensusFoodEntryResponse;
import com.salob.proto.foodEntry.FoodEntryServiceGrpc;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class FoodEntryGrpcHandler extends FoodEntryServiceGrpc.FoodEntryServiceImplBase {
	private final FoodEntryRepository foodEntryRepo;
	private final ConfidenceAlgorithm confidenceAlgo;

	@Override
	public void getConsensusFoodEntry(ConsensusFoodEntryRequest request,
			StreamObserver<ConsensusFoodEntryResponse> responseObserver) {
		try {
			List<FoodEntry> entriesFromEatery = foodEntryRepo.findByFoodNameAndEateryNameFuzzy(request.getFoodName(),
					request.getEateryName());

			double bestConf = -999999;
			FoodEntry bestEntry = null;
			for (FoodEntry foodEntry : entriesFromEatery) {
				double conf = confidenceAlgo.getFinalConfidence(foodEntry);
				if (conf > bestConf) {
					bestConf = conf;
					bestEntry = foodEntry;
				}
			}
			if (bestEntry == null) {
				log.warn("No entries found for: {} at {}", request.getFoodName(), request.getEateryName());
				throw new RuntimeException(
						"No entries found for food " + request.getFoodName() + " at eatery " + request.getEateryName());
			}

			log.info("Returning consensus price for {} from eatery {}: {} SGD (Confidence: {:.1f}%)",
					request.getFoodName(), request.getEateryName(), bestEntry.getSgCents() / 100.0, bestConf * 100);

			var response = ConsensusFoodEntryResponse.newBuilder().setSgCents(bestEntry.getSgCents()).build();
			responseObserver.onNext(response);
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.error("Error fetching consensus food entry", e);
			responseObserver.onError(Status.INTERNAL.withCause(e).asException());
		}
	}
}
