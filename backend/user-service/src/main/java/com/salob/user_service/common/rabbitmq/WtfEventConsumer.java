package com.salob.user_service.common.rabbitmq;

import com.salob.proto.common.RabbitMQConstants;
import com.salob.proto.events.FoodEntryFlaggedEvent;
import com.salob.proto.events.FoodEntrySubmittedEvent;
import com.salob.proto.events.VoteEvent;

import com.salob.user_service.common.IdempotencyManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class WtfEventConsumer {
	private final WtfRecalculationService recalculationService;
	private final IdempotencyManager idempotencyManager;

	@RabbitListener(queues = RabbitMQConstants.QUEUE_USER_WTF_VOTE_CAST)
	@Transactional
	public void handleVote(VoteEvent event) {
		String eventId = event.eventId().toString();
		if (idempotencyManager.hasKey(eventId)) {
			log.info("Skipping duplicate VoteEvent: eventId={}", eventId);
			return;
		}

		try {
			idempotencyManager.setKey(eventId, java.time.Duration.ofHours(1));
			recalculationService.applyVote(event);
			log.info("Successfully processed VoteEvent: eventId={}", eventId);
		} catch (Exception e) {
			log.error("Failed to process VoteEvent: eventId={}", eventId, e);
			throw e;
		}
	}

	@RabbitListener(queues = RabbitMQConstants.QUEUE_USER_WTF_ENTRY_CREATED)
	@Transactional
	public void handleEntrySubmitted(FoodEntrySubmittedEvent event) {
		String eventId = event.eventId().toString();
		if (idempotencyManager.hasKey(eventId)) {
			log.info("Skipping duplicate FoodEntrySubmittedEvent: eventId={}", eventId);
			return;
		}
		try {
			idempotencyManager.setKey(eventId, java.time.Duration.ofHours(1));
			recalculationService.applyEntrySubmitted(event);
			log.info("Successfully processed FoodEntrySubmittedEvent: eventId={}", eventId);
		} catch (Exception e) {
			log.error("Failed to process FoodEntrySubmittedEvent: eventId={}", eventId, e);
			throw e;
		}
	}

	@RabbitListener(queues = RabbitMQConstants.QUEUE_USER_WTF_FLAG_RAISED)
	@Transactional
	public void handleFlagRaised(FoodEntryFlaggedEvent event) {
		String eventId = event.eventId().toString();
		if (idempotencyManager.hasKey(eventId)) {
			log.info("Skipping duplicate FoodEntryFlaggedEvent: eventId={}", eventId);
			return;
		}
		try {
			idempotencyManager.setKey(eventId, java.time.Duration.ofHours(1));
			recalculationService.applyFlagRaised(event);
			log.info("Successfully processed FoodEntryFlaggedEvent: eventId={}", eventId);
		} catch (Exception e) {
			log.error("Failed to process FoodEntryFlaggedEvent: eventId={}", eventId, e);
			throw e;
		}
	}
}
