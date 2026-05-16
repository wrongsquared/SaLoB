package com.salob.food_service.seeding;

import com.salob.food_service.api._domain.*;
import com.salob.food_service.api.eatery.EateryRepository;
import com.salob.food_service.api.eatery_type.EateryTypeRepository;
import com.salob.food_service.api.food_entry.FoodEntryRepository;
import com.salob.food_service.api.food_entry_flag.FoodEntryFlagRepository;
import com.salob.food_service.api.food_entry_vote.FoodEntryVoteRepository;
import com.salob.food_service.api.food.FoodRepository;
import com.salob.food_service.seeding.seeders.*;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.salob.proto.user.UserIDsRequest;
import com.salob.proto.user.UserIDsResponse;
import com.salob.proto.user.UserServiceGrpc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Orchestrates all seeders in explicit order.
 *
 * Seeding order matters:
 * 1. EateryTypes (no dependencies)
 * 2. Eateries (depends on types)
 * 3. Foods (no dependencies)
 * 4. FoodEntries (depends on foods + eateries)
 * 5. FoodEntryVotes (depends on food entries + user IDs)
 *
 * Reset behavior:
 * - If app.seed.reset=true, deletes all data before reseeding (fast for stress tests)
 * - Otherwise, idempotent seeding (safe to run multiple times)
 */
@Slf4j
@Component
@Profile("dev")
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class SeedDataRunner implements CommandLineRunner {
    @GrpcClient("user-service")
    private UserServiceGrpc.UserServiceBlockingStub userServiceStub;
    //-------------------------------------------------------------
    private final EateryTypeSeeder eateryTypeSeeder;
    private final EateryTypeRepository eateryTypeRepo;
    //-------------------------------------------------------------
    private final EaterySeeder eaterySeeder;
    private final EateryRepository eateryRepo;
    //-------------------------------------------------------------
    private final FoodSeeder foodSeeder;
    private final FoodRepository foodRepo;
    //-------------------------------------------------------------
    private final FoodEntrySeeder foodEntrySeeder;
    private final FoodEntryRepository foodEntryRepo;
    //-------------------------------------------------------------
    private final FoodEntryVoteSeeder foodEntryVoteSeeder;
    private final FoodEntryVoteRepository foodEntryVoteRepo;
    //-------------------------------------------------------------
    private final FoodEntryFlagSeeder foodEntryFlagSeeder;
    private final FoodEntryFlagRepository foodEntryFlagRepo;
    //-------------------------------------------------------------

    @Override
    public void run(String... args) {
        resetDatabase();
        log.info("Starting seeding process...");

        List<UUID> userIDs = retrieveAllIDsFromUserService();
        List<EateryType> eateryTypes = eateryTypeSeeder.seed();
        List<Eatery> eateries = eaterySeeder.seed(eateryTypes);
        List<Food> foods = foodSeeder.seed();
        List<FoodEntry> foodEntries = foodEntrySeeder.seed(userIDs, foods, eateries);
        foodEntryVoteSeeder.seed(userIDs, foodEntries);
        foodEntryFlagSeeder.seed(userIDs, foodEntries);

        log.info("Seeding complete");
    }

    private void resetDatabase() {
        try {
            foodEntryFlagRepo.deleteAllInBatch();
            log.debug("Deleted food entry flags");
        } catch (Exception e) {
            log.warn("Failed to delete food entry flags: {}", e.getMessage());
        }

        try {
            foodEntryVoteRepo.deleteAllInBatch();
            log.debug("Deleted food entry votes");
        } catch (Exception e) {
            log.warn("Failed to delete food entry votes: {}", e.getMessage());
        }

        try {
            foodEntryRepo.deleteAllInBatch();
            log.debug("Deleted food entries");
        } catch (Exception e) {
            log.warn("Failed to delete food entries: {}", e.getMessage());
        }

        try {
            eateryRepo.deleteAllInBatch();
            log.debug("Deleted eateries");
        } catch (Exception e) {
            log.warn("Failed to delete eateries: {}", e.getMessage());
        }

        try {
            foodRepo.deleteAllInBatch();
            log.debug("Deleted foods");
        } catch (Exception e) {
            log.warn("Failed to delete foods: {}", e.getMessage());
        }

        try {
            eateryTypeRepo.deleteAllInBatch();
            log.debug("Deleted eatery types");
        } catch (Exception e) {
            log.warn("Failed to delete eatery types: {}", e.getMessage());
        }
    }

    /**
     * User-Service MUST be running, we're making a gRPC call there to fetch all the user IDs
     */
    private List<UUID> retrieveAllIDsFromUserService() {
        try {
            UserIDsResponse allUserIDs = userServiceStub.getAllUserIDs(UserIDsRequest.newBuilder().build());
            return allUserIDs.getIdsList().stream()
                    .map(uuidStr -> {
                        try {
                            return UUID.fromString(uuidStr);
                        } catch (IllegalArgumentException e) {
                            log.warn("Invalid UUID string from user service: {}", uuidStr);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception e) {
            log.error("Failed to fetch user IDs, ensure that user-service is running. Error: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
