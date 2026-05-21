package com.salob.user_service.common.rabbitmq;

import com.salob.proto.events.WtfEvent;
import com.salob.user_service.api._domain.User;
import com.salob.user_service.api.users.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Recalculates a user's WTF score based on their denormalized stats.
 *
 * Called by {@link WtfEventConsumer} when a WTF-related event arrives.
 * Uses atomic SQL increments for counters, then recomputes the score.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WtfRecalculationService {

    // WTF formula parameters (tunable)
    static final int TENURE_DAYS_MAX = 365;
    static final int VOTE_SLOPE_DIVIDER = 10;
    static final int VOLUME_SUBMISSIONS_MAX = 20;

    static final double W_TENURE = 0.15;
    static final double W_VOTE = 0.40;
    static final double W_FLAG = 0.30;
    static final double W_VOLUME = 0.15;

    static final int ACTIVITY_DECAY_DAYS = 180;
    static final double ACTIVITY_MIN_MULTIPLIER = 0.5;

    private final UserRepository userRepo;

    /**
     * Apply a WTF event: update the target user's counters and recalculate their WTF,
     * then update the actor's lastActivityAt (may be same or different user).
     *
     * Rule for lastActivityAt: the ACTOR performed the action (voted, submitted, flagged),
     * so their lastActivityAt gets updated. The target user only gets their lastActivityAt
     * updated when they are also the actor (i.e., ENTRY_SUBMITTED where actor == target).
     */
    @Transactional
    public void applyEvent(WtfEvent event) {
        // Step 1: Update target user's counters and recalculate WTF
        User targetUser = userRepo
            .findById(event.targetUserId())
            .orElseThrow(() -> new RuntimeException("Target user not found: " + event.targetUserId()));

        switch (event.type()) {
            case "ENTRY_SUBMITTED" -> targetUser.setTotalSubmissions(targetUser.getTotalSubmissions() + 1);
            case "VOTE_CAST" -> {
                if (Boolean.TRUE.equals(event.isUpvote())) {
                    targetUser.setUpvotesReceived(targetUser.getUpvotesReceived() + 1);
                } else {
                    targetUser.setDownvotesReceived(targetUser.getDownvotesReceived() + 1);
                }
            }
            case "FLAG_RAISED" -> targetUser.setAnomaliesFlagged(targetUser.getAnomaliesFlagged() + 1);
            default -> log.warn("Unknown WTF event type: {}", event.type());
        }

        // Update lastActivityAt for the target if they are also the actor
        if (event.actorId() != null && event.actorId().equals(event.targetUserId())) {
            targetUser.setLastActivityAt(Instant.now());
        }

        targetUser.setWtfScore(computeWtfScore(targetUser));
        userRepo.save(targetUser);
        log.info(
            "WTF recalculated for user {}: {} (type={})",
            targetUser.getId(),
            targetUser.getWtfScore(),
            event.type()
        );

        // Step 2: If the actor is a different user, update their lastActivityAt
        if (event.actorId() != null && !event.actorId().equals(event.targetUserId())) {
            userRepo.findById(event.actorId()).ifPresent(actor -> {
                actor.setLastActivityAt(Instant.now());
                userRepo.save(actor);
            });
        }
    }

    /**
     * TODO: Maybe cache the WTF score??
     * Compute the WTF score for a user based on their current stats.
     */
    public double computeWtfScore(User user) {
        long daysSinceRegistration = ChronoUnit.DAYS.between(user.getCreatedAt(), Instant.now());
        long daysSinceLastActivity =
            user.getLastActivityAt() != null
                ? ChronoUnit.DAYS.between(user.getLastActivityAt(), Instant.now())
                : daysSinceRegistration;

        double tenureScore = Math.min((double) daysSinceRegistration / TENURE_DAYS_MAX, 1.0) * 100.0;

        double voteScore =
            50.0 +
            50.0 * Math.tanh((double) (user.getUpvotesReceived() - user.getDownvotesReceived()) / VOTE_SLOPE_DIVIDER);

        double flagScore =
            user.getTotalSubmissions() > 0
                ? 100.0 * (1.0 - (double) user.getAnomaliesFlagged() / user.getTotalSubmissions())
                : 100.0;

        double volumeScore = Math.min((double) user.getTotalSubmissions() / VOLUME_SUBMISSIONS_MAX, 1.0) * 100.0;

        double rawScore = W_TENURE * tenureScore + W_VOTE * voteScore + W_FLAG * flagScore + W_VOLUME * volumeScore;

        double activityMultiplier =
            1.0 - (1.0 - ACTIVITY_MIN_MULTIPLIER) * Math.min((double) daysSinceLastActivity / ACTIVITY_DECAY_DAYS, 1.0);

        return Math.max(0, Math.min(100, rawScore * activityMultiplier));
    }
}
