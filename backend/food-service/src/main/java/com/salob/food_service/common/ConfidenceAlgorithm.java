package com.salob.food_service.common;

import com.salob.food_service.api._domain.FoodEntry;
import com.salob.food_service.api._domain.FoodEntryVote;
import com.salob.proto.user.UserServiceGrpc;
import com.salob.proto.user.UserWtfBatchRequest;
import com.salob.proto.user.UserWtfBatchResponse;
import com.salob.proto.user.UserWtfBatchResponseItem;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * const finalConfidenceScore = foodEntry => {
 *     // Refer to NOTE below regarding the 'votesRequiredForMaxScore'
 *     const baseVoteValue = 100 / votesRequiredForMaxScore;
 *     let conf = 0;
 *
 *     for (const vote of foodEntry.votes) {
 *         const actualVoteValue = vote.isUpvote ? baseVoteValue : -baseVoteValue;
 *         conf += actualVoteValue * computeMultiplier(vote.voter.WTFScore); // Lagrange
 *     }
 *
 *     const decayedConf = decay(foodEntry.ageInYears);
 *     return clamp(0, 100, decayedConf);
 * };
 */
@Component
public class ConfidenceAlgorithm {

    /**
     * NOTE: 'votesRequiredForMaxScore' is the number of upvotes that are required from
     *       people (each with a WTF score of 50) on a food entry (with no downvotes!)
     *       to reach a confidence score of 100 (not taking into account time decay).
     */
    private static final int VOTES_REQUIRED_FOR_MAX_SCORE = 150; // User-adjustable

    @GrpcClient("user-service")
    private UserServiceGrpc.UserServiceBlockingStub userService;

    public double computeFinalConfidence(FoodEntry foodEntry) {
        List<FoodEntryVote> votes = foodEntry.getVotes();

        Map<UUID, Double> wtfScores = batchFetchWtfScores(votes);

        double valuePerVote = 100.0 / VOTES_REQUIRED_FOR_MAX_SCORE;
        double rawConfidence = 0;
        for (FoodEntryVote vote : votes) {
            double wtfScoreOfVoter = wtfScores.getOrDefault(vote.getVoterId(), 50.0);
            double multiplier = convertWtfScoreToMultiplier(wtfScoreOfVoter);
            rawConfidence += (vote.isUpvote() ? valuePerVote : -valuePerVote) * multiplier;
        }
        double foodEntryAgeInYears = Duration.between(foodEntry.getCreatedAt(), Instant.now()).toDays() / 365.25;
        return applyConcaveTimeDecay(rawConfidence, foodEntryAgeInYears);
    }

    private Map<UUID, Double> batchFetchWtfScores(List<FoodEntryVote> votes) {
        List<String> voterIds = votes.stream()
                .map(v -> v.getVoterId().toString())
                .distinct()
                .toList();

        if (voterIds.isEmpty()) {
            return new HashMap<>();
        }

        UserWtfBatchRequest req = UserWtfBatchRequest.newBuilder()
                .addAllUserIds(voterIds)
                .build();
        UserWtfBatchResponse res = userService.getUserWtfScoreBatch(req);

        Map<UUID, Double> wtfMap = new HashMap<>();
        for (UserWtfBatchResponseItem item : res.getItemsList()) {
            wtfMap.put(UUID.fromString(item.getUserId()), item.getWtfScore());
        }
        return wtfMap;
    }

    /**
     * Package-private for testing purposes.
     * Converts a WTF score [0, 100] to a multiplier [0.1, 2.5] using Lagrange interpolation.
     */
    double convertWtfScoreToMultiplier(double wtfScore) {
        double clampedScore = Math.clamp(wtfScore, 0, 100);
        double l0 = ((clampedScore - 50) * (clampedScore - 100)) / ((-50) * (-100));
        double l1 = ((clampedScore - 0) * (clampedScore - 100)) / ((50) * (50 - 100));
        double l2 = ((clampedScore - 0) * (clampedScore - 50)) / ((100) * (100 - 50));
        return (0.1 * l0) + l1 + (2.5 * l2);
    }

    /**
     * Package-private for testing purposes.
     * Applies concave time decay to confidence scores based on age in years.
     */
    double applyConcaveTimeDecay(double preDecayConfidence, double foodEntryAgeInYears) {
        double clampedYears = Math.clamp(foodEntryAgeInYears, 0, 10); // Max decay at 10 years anyway

        // Amount that is 'preserved', 0-100. 100 means no decay, 0 is full decay
        double nonDecayPercent = -1 * Math.pow(clampedYears, 2) + 100;

        // Apply the decay, normalize to "0-1" range to use as a multiplier
        double result = preDecayConfidence * (nonDecayPercent / 100);

        // Clamp to [0, 100] to ensure result is never negative and never exceeds 100
        return Math.clamp(result, 0, 100);
    }
}
