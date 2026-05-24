package com.salob.user_service.common.rabbitmq;

import com.salob.proto.common.RabbitMQConstants;
import com.salob.proto.events.FoodEntryFlaggedEvent;
import com.salob.proto.events.FoodEntrySubmittedEvent;
import com.salob.proto.events.VoteEvent;
import com.salob.user_service.api._domain.ProcessedEvent;
import com.salob.user_service.api._domain.ProcessedEventRepository;
import java.time.Instant;
import java.util.UUID;
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
	private final ProcessedEventRepository processedEventRepo;

	@RabbitListener(queues = RabbitMQConstants.QUEUE_WTF_VOTE_CAST)
	@Transactional
	public void handleVote(VoteEvent event) {
		UUID eventId = UUID.fromString(event.eventId());
		if (processedEventRepo.existsById(eventId)) {
			log.info("Skipping duplicate VoteEvent: eventId={}", eventId);
			return;
		}
		try {
			recalculationService.applyVote(event);
			processedEventRepo.save(new ProcessedEvent(eventId, Instant.now()));
			log.info("Successfully processed VoteEvent: eventId={}", eventId);
		} catch (Exception e) {
			log.error("Failed to process VoteEvent: eventId={}", eventId, e);
			throw e;
		}
	}

	@RabbitListener(queues = RabbitMQConstants.QUEUE_WTF_ENTRY_CREATED)
	@Transactional
	public void handleEntrySubmitted(FoodEntrySubmittedEvent event) {
		UUID eventId = UUID.fromString(event.eventId());
		if (processedEventRepo.existsById(eventId)) {
			log.info("Skipping duplicate FoodEntrySubmittedEvent: eventId={}", eventId);
			return;
		}
		try {
			recalculationService.applyEntrySubmitted(event);
			processedEventRepo.save(new ProcessedEvent(eventId, Instant.now()));
			log.info("Successfully processed FoodEntrySubmittedEvent: eventId={}", eventId);
		} catch (Exception e) {
			log.error("Failed to process FoodEntrySubmittedEvent: eventId={}", eventId, e);
			throw e;
		}
	}

	@RabbitListener(queues = RabbitMQConstants.QUEUE_WTF_FLAG_RAISED)
	@Transactional
	public void handleFlagRaised(FoodEntryFlaggedEvent event) {
		UUID eventId = UUID.fromString(event.eventId());
		if (processedEventRepo.existsById(eventId)) {
			log.info("Skipping duplicate FoodEntryFlaggedEvent: eventId={}", eventId);
			return;
		}
		try {
			recalculationService.applyFlagRaised(event);
			processedEventRepo.save(new ProcessedEvent(eventId, Instant.now()));
			log.info("Successfully processed FoodEntryFlaggedEvent: eventId={}", eventId);
		} catch (Exception e) {
			log.error("Failed to process FoodEntryFlaggedEvent: eventId={}", eventId, e);
			throw e;
		}
	}
}
