package com.salob.food_service.common.rabbitmq;

import com.salob.proto.common.RabbitMQConstants;
import com.salob.proto.events.WtfEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class WtfEventPublisher {

	private final RabbitTemplate rabbitTemplate;

	public void publishEntryCreated(UUID submitterId) {
		publish(new WtfEvent("ENTRY_SUBMITTED", UUID.randomUUID(), submitterId, submitterId, null),
				RabbitMQConstants.RK_WTF_ENTRY_CREATED);
	}

	public void publishVoteCast(UUID voterId, UUID entryOwnerId, boolean isUpvote) {
		publish(new WtfEvent("VOTE_CAST", UUID.randomUUID(), voterId, entryOwnerId, isUpvote),
				RabbitMQConstants.RK_WTF_VOTE_CAST);
	}

	public void publishFlagRaised(UUID flaggerId, UUID entryOwnerId) {
		publish(new WtfEvent("FLAG_RAISED", UUID.randomUUID(), flaggerId, entryOwnerId, null),
				RabbitMQConstants.RK_WTF_FLAG_RAISED);
	}

	private void publish(WtfEvent event, String routingKey) {
		log.info("Publishing WTF event: type={}, eventId={}, routingKey={}", event.type(), event.eventId(), routingKey);
		rabbitTemplate.convertAndSend(RabbitMQConstants.EVENTS_EXCHANGE, routingKey, event);
	}
}
