package com.salob.food_service.common.rabbitmq;

import com.salob.food_service.api.food_entry.FoodEntryRepository;
import com.salob.food_service.common.IdempotencyManager;
import com.salob.proto.common.RabbitMQConstants;
import com.salob.proto.events.VoteEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class WtfEventConsumer {
	private final IdempotencyManager idempotencyManager;
	private final FoodEntryRepository foodEntryRepo;

	@RabbitListener(queues = RabbitMQConstants.QUEUE_FOOD_WTF_VOTE_CAST)
	@Transactional
	public void handleVoteEvent(VoteEvent event) {
		String eventId = event.eventId().toString();
		if (idempotencyManager.hasKey(eventId)) {
			log.info("Duplicate event received, ignoring: eventId={}", event.eventId());
			return;
		}

		try {
			idempotencyManager.setKey(eventId, Duration.ofDays(3));
			switch (event.voteType()) {
				case UPVOTE -> foodEntryRepo.modifyUpvoteCount(event.idOfEntryVotedOn(), 1);
				case DOWNVOTE -> foodEntryRepo.modifyDownvoteCount(event.idOfEntryVotedOn(), 1);
				case UPVOTE_REMOVED -> foodEntryRepo.modifyUpvoteCount(event.idOfEntryVotedOn(), -1);
				case DOWNVOTE_REMOVED -> foodEntryRepo.modifyDownvoteCount(event.idOfEntryVotedOn(), -1);
				case UPVOTE_TO_DOWNVOTE -> {
					foodEntryRepo.modifyUpvoteCount(event.idOfEntryVotedOn(), 1);
					foodEntryRepo.modifyDownvoteCount(event.idOfEntryVotedOn(), -1);
				}
				case DOWNVOTE_TO_UPVOTE -> {
					foodEntryRepo.modifyDownvoteCount(event.idOfEntryVotedOn(), -1);
					foodEntryRepo.modifyUpvoteCount(event.idOfEntryVotedOn(), 1);
				}
			};
		} catch (Exception e) {
			idempotencyManager.releaseKey(eventId); // If crash, release key so can retry processing
		}
	}
}
