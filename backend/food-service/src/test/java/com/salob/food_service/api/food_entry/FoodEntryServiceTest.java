package com.salob.food_service.api.food_entry;

import com.salob.food_service.api._domain.Eatery;
import com.salob.food_service.api._domain.Food;
import com.salob.food_service.api._domain.FoodEntry;
import com.salob.food_service.api.eatery.EateryRepository;
import com.salob.food_service.api.food.FoodRepository;
import com.salob.food_service.api.food_entry.dto.FoodEntryDetailedDTO;
import com.salob.food_service.api.food_entry.dto.FoodEntryHistoricalDTO;
import com.salob.food_service.api.food_entry.dto.FoodEntrySubmissionRequest;
import com.salob.food_service.common.ConfidenceAlgorithm;
import com.salob.food_service.storage.minio.MinioStorageService;
import com.salob.proto.user.UserDetailsRequest;
import com.salob.proto.user.UserDetailsResponse;
import com.salob.proto.user.UserServiceGrpc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/*
 * =============================================================================
 * WHAT THIS TEST TEACHES
 * =============================================================================
 *
 * 1. @GrpcClient field injection — FoodEntryService has a field-level gRPC stub
 *    (@GrpcClient private UserServiceGrpc.UserServiceBlockingStub userServiceStub).
 *    This is NOT constructor-injected (it's set by Spring after construction).
 *    In tests, we use ReflectionTestUtils.setField() to inject it manually.
 *
 * 2. ArgumentCaptor — captures the argument passed to a mock so we can
 *    inspect it after the fact. Useful when the argument is constructed
 *    inside the method under test (like the gRPC request).
 *
 * 3. Mocking protobuf responses — UserDetailsResponse is a protobuf-generated
 *    class with a builder pattern. We mock the gRPC stub to return a
 *    pre-built response.
 *
 * 4. JPA getReferenceById — this is a Hibernate proxy pattern. The method
 *    does NOT query the database; it creates a lazy-loaded proxy. In tests
 *    without a database, the proxy still works because we never access
 *    fields of the proxy — we just pass it to JPA's save().
 *    For testing, we verify the proxy was used (verify the call) rather
 *    than checking its fields.
 * =============================================================================
 */

@ExtendWith(MockitoExtension.class)
class FoodEntryServiceTest {

    @Mock private FoodEntryRepository foodEntryRepo;
    @Mock private EateryRepository eateryRepo;
    @Mock private FoodRepository foodRepo;
    @Mock private ConfidenceAlgorithm confidenceAlgo;
    @Mock private MinioStorageService minioService;
    @Mock private UserServiceGrpc.UserServiceBlockingStub userServiceStub;

    private FoodEntryService foodEntryService;

    private UUID foodEntryId;
    private UUID eateryId;
    private UUID foodId;
    private UUID submitterId;
    private Food testFood;
    private Eatery testEatery;
    private FoodEntry testEntry;

    @BeforeEach
    void setUp() {
        foodEntryService = new FoodEntryService(
                foodEntryRepo, eateryRepo, foodRepo, confidenceAlgo, minioService
        );

        /*
         * ReflectionTestUtils.setField() — sets a private field on an object.
         * First arg: the target object
         * Second arg: the field name (string)
         * Third arg: the value to set
         *
         * This is necessary because @GrpcClient is FIELD-level injection,
         * NOT constructor injection. Spring sets it after the constructor
         * runs. We simulate that here.
         *
         * Without this, userServiceStub would be null, and calling
         * userServiceStub.getUserDetails(...) would NPE.
         */
        ReflectionTestUtils.setField(foodEntryService, "userServiceStub", userServiceStub);

        foodEntryId = UUID.randomUUID();
        eateryId = UUID.randomUUID();
        foodId = UUID.randomUUID();
        submitterId = UUID.randomUUID();

        testFood = Food.builder().label("Chicken Rice").photoObjKey("chicken-key").build();
        testFood.setId(foodId);

        testEatery = Eatery.builder()
                .name("Test Hawker")
                .address("1 Test Street")
                .build();
        testEatery.setId(eateryId);

        testEntry = FoodEntry.builder()
                .food(testFood)
                .eatery(testEatery)
                .sgCents(400)
                .upvoteCount(10)
                .downvoteCount(2)
                .submitterId(submitterId)
                .build();
        testEntry.setId(foodEntryId);
        testEntry.setCreatedAt(Instant.now());
    }

    // =========================================================================
    // SECTION 1: getFoodEntryHistoricalData
    //
    // This is the most complex method in the service. It:
    //   1. Fetches the target FoodEntry
    //   2. Fetches all entries of the same food from the same eatery
    //   3. Filters by startDate
    //   4. Computes confidence for each entry
    //   5. Picks the consensus (highest confidence)
    //   6. Collects benchmark entries from the consensus date
    //   7. Assembles a nested DTO
    // =========================================================================

    @Test
    void getFoodEntryHistoricalData_returnsHistoricalDTO() {
        Instant now = Instant.now();
        Instant startDate = now.minus(java.time.Duration.ofDays(30));

        /*
         * Create a second entry (older, lower confidence) to test
         * that the method picks the consensus correctly.
         */
        FoodEntry olderEntry = FoodEntry.builder()
                .food(testFood)
                .eatery(testEatery)
                .sgCents(350)
                .upvoteCount(5)
                .downvoteCount(1)
                .submitterId(submitterId)
                .build();
        olderEntry.setId(UUID.randomUUID());
        olderEntry.setCreatedAt(now.minus(java.time.Duration.ofDays(15)));

        testEntry.setCreatedAt(now.minus(java.time.Duration.ofDays(5)));

        when(foodEntryRepo.findById(foodEntryId)).thenReturn(Optional.of(testEntry));
        when(foodEntryRepo.findByFood_IdAndEatery_Id(foodId, eateryId))
                .thenReturn(List.of(olderEntry, testEntry));
        when(confidenceAlgo.computeFinalConfidence(olderEntry)).thenReturn(30.0);
        when(confidenceAlgo.computeFinalConfidence(testEntry)).thenReturn(80.0);
        /*
         * Mock the benchmark entries query (findByEatery_IdAndCreatedAtBetween)
         * This is called for entries on the consensus date (testEntry's date).
         */
        when(foodEntryRepo.findByEatery_IdAndCreatedAtBetween(
                eq(eateryId), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(testEntry));

        when(minioService.getPresignedUrl("chicken-key", Duration.ofMinutes(30)))
                .thenReturn("https://presigned.url/photo");

        /*
         * toDetailed() calls userServiceStub.getUserDetails() for the consensus
         * entry. Without this mock, Mockito returns null → NPE on .getUsername().
         */
        UserDetailsResponse userDetails = UserDetailsResponse.newBuilder()
                .setUserId(submitterId.toString())
                .setUsername("testuser")
                .build();
        when(userServiceStub.getUserDetails(any(UserDetailsRequest.class)))
                .thenReturn(userDetails);

        FoodEntryHistoricalDTO result = foodEntryService.getFoodEntryHistoricalData(foodEntryId, startDate);

        assertNotNull(result);
        /*
         * Records use accessor methods without "get" prefix.
         * FoodEntryHistoricalDTO is a record: result.foodName(), not getFoodName().
         */
        assertEquals("Chicken Rice", result.foodName());
        assertEquals(400, result.sgCentsConsensusPrice());
        assertEquals(eateryId, result.eateryId());
        assertEquals("1 Test Street", result.eateryAddress());
        assertFalse(result.availableDates().isEmpty());
        assertEquals(1, result.benchmarkDateEntries().size());

        /*
         * Verify the consensus entry details are present.
         */
        assertNotNull(result.consensusEntry());
        assertEquals(foodEntryId, result.consensusEntry().foodEntryId());
    }

    // =========================================================================
    // SECTION 2: getFoodEntryDetailed
    //
    // This method calls user-service via gRPC to enrich the entry with
    // submitter details (username, photo, WTF score, tenure).
    // =========================================================================

    @Test
    void getFoodEntryDetailed_returnsDetailedDTOWithUserInfo() {
        when(foodEntryRepo.findById(foodEntryId)).thenReturn(Optional.of(testEntry));

        /*
         * Build a protobuf response. UserDetailsResponse is generated by
         * protobuf — it uses the builder pattern from protobuf, NOT Lombok.
         *
         * .setUserId() / .setUsername() etc. — protobuf's standard pattern.
         * .build() — finalizes the message (immutable after this).
         */
        UserDetailsResponse userDetails = UserDetailsResponse.newBuilder()
                .setUserId(submitterId.toString())
                .setUsername("testuser")
                .setPhotoUrl("https://avatar.url")
                .setWtfScore(60.0)
                .setTenureDays(120)
                .build();

        when(userServiceStub.getUserDetails(any(UserDetailsRequest.class)))
                .thenReturn(userDetails);

        when(minioService.getPresignedUrl("chicken-key", Duration.ofMinutes(30)))
                .thenReturn("https://presigned.url/photo");

        when(foodEntryRepo.countBySubmitterId(submitterId)).thenReturn(5L);

        FoodEntryDetailedDTO result = foodEntryService.getFoodEntryDetailed(foodEntryId);

        assertNotNull(result);
        /*
         * FoodEntryDetailedDTO is also a record.
         */
        assertEquals(foodEntryId, result.foodEntryId());
        assertEquals(submitterId, result.submitterId());
        assertEquals("testuser", result.submitterUsername());
        assertEquals("https://presigned.url/photo", result.foodPhotoPresignedUrl());
        assertEquals(60.0, result.submitterWtfScore(), 1e-9);
        assertEquals(120, result.submitterTenureDays());
        assertEquals(5, result.submitterEntriesSubmitted());

        /*
         * ArgumentCaptor — "captures" the argument passed to a mock method.
         * We use it here to verify the EXACT gRPC request that was sent.
         *
         * Pattern:
         *   1. Declare: ArgumentCaptor<UserDetailsRequest> captor = ArgumentCaptor.forClass(...)
         *   2. Verify:  verify(mock).method(captor.capture())
         *   3. Inspect: captor.getValue() contains the actual argument used
         */
        ArgumentCaptor<UserDetailsRequest> requestCaptor =
                ArgumentCaptor.forClass(UserDetailsRequest.class);
        verify(userServiceStub).getUserDetails(requestCaptor.capture());

        UserDetailsRequest sentRequest = requestCaptor.getValue();
        assertEquals(submitterId.toString(), sentRequest.getUserId());
    }

    // =========================================================================
    // SECTION 3: submitFoodEntry
    //
    // Tests the submission flow. The service uses JPA's getReferenceById()
    // which creates proxies (no DB query) — in a test without a DB, the
    // proxies are still created successfully.
    // =========================================================================

    @Test
    void submitFoodEntry_createsEntryAndSaves() {
        UUID submitterId = UUID.randomUUID();
        FoodEntrySubmissionRequest req = new FoodEntrySubmissionRequest(eateryId, foodId, 500);

        /*
         * We mock getReferenceById to return fake entities.
         * These are just stubs — their fields are never read by
         * submitFoodEntry (it only passes them to the FoodEntry builder).
         */
        Eatery eateryProxy = mock(Eatery.class);
        Food foodProxy = mock(Food.class);
        when(eateryRepo.getReferenceById(eateryId)).thenReturn(eateryProxy);
        when(foodRepo.getReferenceById(foodId)).thenReturn(foodProxy);

        foodEntryService.submitFoodEntry(submitterId, req);

        /*
         * ArgumentCaptor for FoodEntry — capture what was saved.
         */
        ArgumentCaptor<FoodEntry> entryCaptor = ArgumentCaptor.forClass(FoodEntry.class);
        verify(foodEntryRepo).save(entryCaptor.capture());

        FoodEntry savedEntry = entryCaptor.getValue();
        assertEquals(foodProxy, savedEntry.getFood());
        assertEquals(eateryProxy, savedEntry.getEatery());
        assertEquals(500, savedEntry.getSgCents());
        assertEquals(submitterId, savedEntry.getSubmitterId());
        assertEquals(0, savedEntry.getUpvoteCount());
        assertEquals(0, savedEntry.getDownvoteCount());
    }
}
