package com.salob.food_service.common;

import com.salob.food_service.features.food.domain.FoodEntry;
import com.salob.food_service.features.food.domain.FoodEntryVote;
import com.salob.proto.user.UserServiceGrpc;
import com.salob.proto.user.UserWtfRequest;
import com.salob.proto.user.UserWtfResponse;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.UUID;

/**
 * const finalConfidenceScore = foodEntry => {
 *     // Refer to NOTE below regarding the 'votesRequiredForMaxScore'
 *     const baseVoteValue = 100 / votesRequiredForMaxScore;
 *     let conf = 0;
 *
 *     for (const vote of foodEntry.votes) {
 *         const actualVoteValue = vote.isUpvote ? baseVoteValue : -baseVoteValue;
 *         conf += actualVoteValue * computeMultiplier(vote.voter.WTFSScore); // Lagrange
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
        return (new Random().nextDouble()) * 100; // TODO: Implement the actual algorithm instead of random values for testing
//        double valuePerVote = 100 / VOTES_REQUIRED_FOR_MAX_SCORE;
//        double rawConfidence = 0;
//        for (FoodEntryVote vote : foodEntry.getVotes()) {
//            double wtfScoreOfVoter = getVotersWtfScore(vote.getVoterId());
//            double multiplier = convertWtfScoreToMultiplier(wtfScoreOfVoter);
//            rawConfidence += (vote.isUpvote() ? valuePerVote : -valuePerVote) * multiplier;
//        }
//        double foodEntryAgeInYears = Duration.between(foodEntry.getCreatedAt(), Instant.now()).toDays() / 365.25;
//        return applyConcaveTimeDecay(rawConfidence, foodEntryAgeInYears);
    }

    private double convertWtfScoreToMultiplier(double wtfScore) {
        double clampedScore = Math.clamp(0, 100, wtfScore);
        double l0 = ((clampedScore - 50) * (clampedScore - 100)) / ((-50) * (-100));
        double l1 = ((clampedScore - 0) * (clampedScore - 100)) / ((50) * (50 - 100));
        double l2 = ((clampedScore - 0) * (clampedScore - 50)) / ((100) * (100 - 50));
        return (0.1 * l0) + l1 + (2.5 * l2);
    }

    private double applyConcaveTimeDecay(double preDecayConfidence, double foodEntryAgeInYears) {
        double clampedYears = Math.clamp(0, 10, foodEntryAgeInYears); // Max decay at 10 years anyway

        // Amount that is 'preserved', 0-100. 100 means no decay, 0 is full decay
        double nonDecayPercent = -1 * Math.pow(clampedYears, 2) + 100;

        // Appy the decay, normalize to "0-1" range to use as a multiplier
        return preDecayConfidence * (nonDecayPercent / 100);
    }

    private double getVotersWtfScore(UUID voterId) {
        var req = UserWtfRequest.newBuilder().setUserId(voterId.toString()).build();
        UserWtfResponse res = userService.getUserWtfScore(req);
        return res.getWtfScore();
    }
}

