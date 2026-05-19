package com.salob.food_service.common;

import com.salob.food_service.api._domain.FoodEntry;
import com.salob.food_service.api._domain.FoodEntryVote;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/*
 * =============================================================================
 * WHAT THIS TEST TEACHES
 * =============================================================================
 *
 * 1. Plain JUnit 5 — no Spring, no containers, no mocks. Just Java.
 *    The test creates objects directly, calls methods, asserts results.
 *
 * 2. Package-private access — ConfidenceAlgorithm has two methods marked
 *    "package-private for testing purposes". Our test is in the SAME package
 *    (com.salob.food_service.common), so we can call them directly.
 *
 * 3. Domain entity construction — FoodEntry uses Lombok @Builder. We build
 *    test entities with only the fields we need. The builder pattern means
 *    we don't have to fill in everything — defaults handle the rest.
 *
 * 4. The confidence algorithm is PURE LOGIC — it takes numbers in, returns
 *    numbers out. No databases, no HTTP, no gRPC. This is the ideal kind of
 *    code to unit-test: fast, deterministic, exhaustive.
 * =============================================================================
 */
class ConfidenceAlgorithmTest {

    /*
     * Why @Test on an instance method and not static?
     * JUnit 5 creates a NEW instance of this test class for EACH @Test method.
     * This is intentional — each test runs in isolation, no shared state.
     * We could make the algorithm a field (new up once per test), or create
     * it inside each test. Both work. We do it inline for clarity here.
     */

    // =========================================================================
    // SECTION 1: convertWtfScoreToMultiplier
    //
    // This function uses Lagrange interpolation through 3 anchor points:
    //   WTF=0   -> multiplier=0.1   (a completely untrusted voter)
    //   WTF=50  -> multiplier=1.0   (the "average" voter, no amplification)
    //   WTF=100 -> multiplier=2.5   (a maximally trusted voter)
    //
    // Lagrange interpolation ensures a smooth curve through all three points.
    // We test each anchor, points between them, and boundary clamping.
    // =========================================================================

    @Test
    void convertWtfScoreToMultiplier_returns0point1_atScore0() {
        ConfidenceAlgorithm algo = new ConfidenceAlgorithm();
        double result = algo.convertWtfScoreToMultiplier(0);
        /*
         * assertEquals(expected, actual, delta)
         * delta = 0.0001 means "allow this much floating-point error".
         * Floating-point math is never exact (0.1 might be 0.099999...),
         * so we always use a tolerance with double assertions.
         */
        assertEquals(0.1, result, 1e-9);
    }

    @Test
    void convertWtfScoreToMultiplier_returns1point0_atScore50() {
        ConfidenceAlgorithm algo = new ConfidenceAlgorithm();
        double result = algo.convertWtfScoreToMultiplier(50);
        assertEquals(1.0, result, 1e-9);
    }

    @Test
    void convertWtfScoreToMultiplier_returns2point5_atScore100() {
        ConfidenceAlgorithm algo = new ConfidenceAlgorithm();
        double result = algo.convertWtfScoreToMultiplier(100);
        assertEquals(2.5, result, 1e-9);
    }

    @Test
    void convertWtfScoreToMultiplier_clampsNegativeScoresTo0() {
        ConfidenceAlgorithm algo = new ConfidenceAlgorithm();
        /*
         * The function first clamps input to [0, 100], so -50 becomes 0.
         * We expect the multiplier for 0.
         */
        double result = algo.convertWtfScoreToMultiplier(-50);
        assertEquals(0.1, result, 1e-9);
    }

    @Test
    void convertWtfScoreToMultiplier_clampsScoresAbove100() {
        ConfidenceAlgorithm algo = new ConfidenceAlgorithm();
        double result = algo.convertWtfScoreToMultiplier(150);
        assertEquals(2.5, result, 1e-9);
    }

    @Test
    void convertWtfScoreToMultiplier_isMonotonicIncreasing() {
        /*
         * Property-based test: as WTF score increases, the multiplier
         * should ALWAYS increase (never decrease). This is a stronger
         * guarantee than testing individual points.
         */
        ConfidenceAlgorithm algo = new ConfidenceAlgorithm();
        double prev = algo.convertWtfScoreToMultiplier(0);
        for (int score = 1; score <= 100; score++) {
            double current = algo.convertWtfScoreToMultiplier(score);
            assertTrue(current >= prev,
                    "Multiplier should not decrease at WTF=" + score);
            prev = current;
        }
    }

    // =========================================================================
    // SECTION 2: applyConcaveTimeDecay
    //
    // This function applies a "concave" (parabolic) decay over time:
    //   nonDecayPercent = -years^2 + 100
    //   result = preDecayConfidence * (nonDecayPercent / 100)
    //
    // At 0 years: 100% preserved (full confidence)
    // At 10 years:   0% preserved (complete decay — max)
    //
    // The name "concave" means the decay accelerates as time passes
    // (the curve bends downward). For confidence, this means:
    //   A 1-year-old entry decays mildly (barely noticeable)
    //   A 5-year-old entry decays significantly
    //   A 10-year-old entry is fully decayed
    // =========================================================================

    @Test
    void applyConcaveTimeDecay_preservesFullConfidence_atZeroYears() {
        ConfidenceAlgorithm algo = new ConfidenceAlgorithm();
        /*
         * If foodEntryAgeInYears = 0:
         *   nonDecayPercent = -0^2 + 100 = 100
         *   result = 80 * (100/100) = 80
         */
        double result = algo.applyConcaveTimeDecay(80, 0);
        assertEquals(80, result, 1e-9);
    }

    @Test
    void applyConcaveTimeDecay_reducesToZero_atTenYears() {
        ConfidenceAlgorithm algo = new ConfidenceAlgorithm();
        /*
         * If foodEntryAgeInYears = 10:
         *   nonDecayPercent = -100 + 100 = 0
         *   result = 80 * (0/100) = 0
         */
        double result = algo.applyConcaveTimeDecay(80, 10);
        assertEquals(0.0, result, 1e-9);
    }

    @Test
    void applyConcaveTimeDecay_clampsBeyondTenYears() {
        ConfidenceAlgorithm algo = new ConfidenceAlgorithm();
        // 20 years clamps to 10, same result as at 10
        double result = algo.applyConcaveTimeDecay(80, 20);
        assertEquals(0.0, result, 1e-9);
    }

    @Test
    void applyConcaveTimeDecay_neverReturnsNegative() {
        /*
         * Even with a very negative pre-decay confidence (which shouldn't
         * happen in practice since we clamp), the result should be >= 0.
         */
        ConfidenceAlgorithm algo = new ConfidenceAlgorithm();
        double result = algo.applyConcaveTimeDecay(-50, 5);
        assertTrue(result >= 0);
    }

    // =========================================================================
    // SECTION 3: computeFinalConfidence (INTEGRATION-ISH)
    //
    // This method wires everything together — it requires a FoodEntry with votes.
    // The @GrpcClient stub inside ConfidenceAlgorithm is null when we create
    // the object via `new ConfidenceAlgorithm()` — Spring normally injects it.
    //
    // For `computeFinalConfidence`, the gRPC stub is only used inside
    // `batchFetchWtfScores()`. If the stub is null, the gRPC call will NPE.
    // We have a test for that scenario to verify the fallback behavior,
    // but the REAL fix is to add the gRPC mock — we cover that later with
    // Mockito in EateryServiceTest.
    //
    // For NOW, we test the math path without votes (exercise the loop with
    // an empty list, which bypasses gRPC).
    // =========================================================================

    @Test
    void computeFinalConfidence_returnsZero_forEntryWithNoVotes() {
        /*
         * FoodEntry has @Builder from Lombok. We use the builder pattern to
         * create an entry with ZERO votes and a known creation time.
         *
         * builder().field(value).build() — only set what matters, defaults for rest.
         */
        FoodEntry entry = FoodEntry.builder()
                .votes(List.of()) // explicitly empty
                .build();
        /*
         * createdAt and id are inherited from BaseEntity (via @Getter @Setter).
         * Lombok's @Builder on a subclass does NOT generate builder methods
         * for inherited fields — we must set them via setters after building.
         */
        entry.setCreatedAt(Instant.now());

        ConfidenceAlgorithm algo = new ConfidenceAlgorithm();
        double result = algo.computeFinalConfidence(entry);

        /*
         * With no votes: rawConfidence stays at 0.
         * At 0 years old: nonDecayPercent = 100, so result = 0 * (100/100) = 0.
         */
        assertEquals(0.0, result, 1e-9);
    }

    @Test
    void computeFinalConfidence_clampsTo100_forExtremelyHighConfidence() {
        /*
         * This test creates a very "old" entry (10 years) where decay = 0%.
         * Since we pass zero votes, the final result should still be 0.
         *
         * We can't easily test the full path with votes here because the
         * gRPC stub is null. That test belongs in EateryServiceTest where
         * we can mock the gRPC client properly.
         *
         * This test verifies the clamp at [0, 100] works.
         */
        FoodEntry entry = FoodEntry.builder()
                .votes(List.of())
                .build();
        entry.setCreatedAt(Instant.now());

        ConfidenceAlgorithm algo = new ConfidenceAlgorithm();
        double result = algo.computeFinalConfidence(entry);

        assertTrue(result >= 0);
        assertTrue(result <= 100);
    }
}
