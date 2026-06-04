package com.salob.food_service.common.rabbitmq;

import com.salob.proto.common.RabbitMQConstants;
import com.salob.proto.events.FoodEntryFlaggedEvent;
import com.salob.proto.events.FoodEntrySubmittedEvent;
import com.salob.proto.events.VoteEvent;
import com.salob.proto.events.VoteType;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WtfEventPublisher {

	private final RabbitTemplate rabbitTemplate;

	public void publishEntryCreated(UUID submitterId) {
		var event = new FoodEntrySubmittedEvent(UUID.randomUUID(), submitterId);
		log.info("Publishing FoodEntrySubmittedEvent: eventId={}", event.eventId());
		rabbitTemplate.convertAndSend(RabbitMQConstants.EVENTS_EXCHANGE, RabbitMQConstants.RK_WTF_ENTRY_CREATED, event);
	}

	public void publishVoteCast(UUID voterId, UUID entryId, UUID entryOwnerId, VoteType voteType) {
		var event = new VoteEvent(UUID.randomUUID(), voterId, entryId, entryOwnerId, voteType);
		log.info("Publishing VoteEvent: eventId={}, voteType={}", event.eventId(), event.voteType());
		rabbitTemplate.convertAndSend(RabbitMQConstants.EVENTS_EXCHANGE, RabbitMQConstants.RK_WTF_VOTE_CAST, event);
	}

	public void publishFlagRaised(UUID flaggerId, UUID entryOwnerId) {
		var event = new FoodEntryFlaggedEvent(UUID.randomUUID(), flaggerId, entryOwnerId);
		log.info("Publishing FoodEntryFlaggedEvent: eventId={}", event.eventId());
		rabbitTemplate.convertAndSend(RabbitMQConstants.EVENTS_EXCHANGE, RabbitMQConstants.RK_WTF_FLAG_RAISED, event);
	}
}
