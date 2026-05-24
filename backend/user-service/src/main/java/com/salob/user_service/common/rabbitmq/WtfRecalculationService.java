package com.salob.user_service.common.rabbitmq;

import com.salob.proto.events.FoodEntryFlaggedEvent;
import com.salob.proto.events.FoodEntrySubmittedEvent;
import com.salob.proto.events.VoteEvent;
import com.salob.proto.events.VoteType;
import com.salob.user_service.api._domain.User;
import com.salob.user_service.api.users.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WtfRecalculationService {

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

	@Transactional
	@CacheEvict(value = "user_wtf", key = "#event.submitterIdOfEntryVotedOn()")
	public void applyVote(VoteEvent event) {
		UUID targetId = UUID.fromString(event.submitterIdOfEntryVotedOn());
		User targetUser = userRepo.findById(targetId)
				.orElseThrow(() -> new RuntimeException("Target user not found: " + targetId));

		switch (event.voteType()) {
			case UPVOTE -> targetUser.setUpvotesReceived(targetUser.getUpvotesReceived() + 1);
			case DOWNVOTE -> targetUser.setDownvotesReceived(targetUser.getDownvotesReceived() + 1);
			case UPVOTE_REMOVED -> targetUser.setUpvotesReceived(targetUser.getUpvotesReceived() - 1);
			case DOWNVOTE_REMOVED -> targetUser.setDownvotesReceived(targetUser.getDownvotesReceived() - 1);
			case UPVOTE_TO_DOWNVOTE -> {
				targetUser.setUpvotesReceived(targetUser.getUpvotesReceived() - 1);
				targetUser.setDownvotesReceived(targetUser.getDownvotesReceived() + 1);
			}
			case DOWNVOTE_TO_UPVOTE -> {
				targetUser.setDownvotesReceived(targetUser.getDownvotesReceived() - 1);
				targetUser.setUpvotesReceived(targetUser.getUpvotesReceived() + 1);
			}
		}

		targetUser.setWtfScore(computeWtfScore(targetUser));
		userRepo.save(targetUser);
		log.info("WTF recalculated for user {}: {} (voteType={})", targetUser.getId(), targetUser.getWtfScore(),
				event.voteType());

		userRepo.findById(UUID.fromString(event.voterId())).ifPresent(actor -> {
			actor.setLastActivityAt(Instant.now());
			userRepo.save(actor);
		});
	}

	@Transactional
	@CacheEvict(value = "user_wtf", key = "#event.submitterId()")
	public void applyEntrySubmitted(FoodEntrySubmittedEvent event) {
		UUID submitterId = UUID.fromString(event.submitterId());
		User user = userRepo.findById(submitterId)
				.orElseThrow(() -> new RuntimeException("User not found: " + submitterId));

		user.setTotalSubmissions(user.getTotalSubmissions() + 1);
		user.setLastActivityAt(Instant.now());
		user.setWtfScore(computeWtfScore(user));
		userRepo.save(user);
		log.info("WTF recalculated for user {}: {} (entry submitted)", user.getId(), user.getWtfScore());
	}

	@Transactional
	@CacheEvict(value = "user_wtf", key = "#event.flaggedEntrySubmitterId()")
	public void applyFlagRaised(FoodEntryFlaggedEvent event) {
		UUID targetId = UUID.fromString(event.flaggedEntrySubmitterId());
		User targetUser = userRepo.findById(targetId)
				.orElseThrow(() -> new RuntimeException("Target user not found: " + targetId));

		targetUser.setAnomaliesFlagged(targetUser.getAnomaliesFlagged() + 1);
		targetUser.setWtfScore(computeWtfScore(targetUser));
		userRepo.save(targetUser);
		log.info("WTF recalculated for user {}: {} (flag raised)", targetUser.getId(), targetUser.getWtfScore());

		userRepo.findById(UUID.fromString(event.flaggerId())).ifPresent(actor -> {
			actor.setLastActivityAt(Instant.now());
			userRepo.save(actor);
		});
	}

	public double computeWtfScore(User user) {
		long daysSinceRegistration = ChronoUnit.DAYS.between(user.getCreatedAt(), Instant.now());
		long daysSinceLastActivity = user.getLastActivityAt() != null
				? ChronoUnit.DAYS.between(user.getLastActivityAt(), Instant.now())
				: daysSinceRegistration;

		double tenureScore = Math.min((double) daysSinceRegistration / TENURE_DAYS_MAX, 1.0) * 100.0;

		double voteScore = 50.0 + 50.0
				* Math.tanh((double) (user.getUpvotesReceived() - user.getDownvotesReceived()) / VOTE_SLOPE_DIVIDER);

		double flagScore = user.getTotalSubmissions() > 0
				? 100.0 * (1.0 - (double) user.getAnomaliesFlagged() / user.getTotalSubmissions())
				: 100.0;

		double volumeScore = Math.min((double) user.getTotalSubmissions() / VOLUME_SUBMISSIONS_MAX, 1.0) * 100.0;

		double rawScore = W_TENURE * tenureScore + W_VOTE * voteScore + W_FLAG * flagScore + W_VOLUME * volumeScore;

		double activityMultiplier = 1.0
				- (1.0 - ACTIVITY_MIN_MULTIPLIER) * Math.min((double) daysSinceLastActivity / ACTIVITY_DECAY_DAYS, 1.0);

		return Math.max(0, Math.min(100, rawScore * activityMultiplier));
	}
}
