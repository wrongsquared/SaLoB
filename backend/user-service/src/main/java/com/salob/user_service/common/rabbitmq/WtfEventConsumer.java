package com.salob.user_service.common.rabbitmq;

import com.salob.proto.common.RabbitMQConstants;
import com.salob.proto.events.WtfEvent;
import com.salob.user_service.api._domain.ProcessedEvent;
import com.salob.user_service.api._domain.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Consumes WTF events from RabbitMQ, checks idempotency, and delegates to
 * {@link WtfRecalculationService}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WtfEventConsumer {

	private final WtfRecalculationService recalculationService;
	private final ProcessedEventRepository processedEventRepo;

	@RabbitListener(queues = RabbitMQConstants.QUEUE_WTF_RECALC)
	@Transactional
	public void handleWtfEvent(WtfEvent event) {
		// Idempotency check: skip if we've already processed this event
		if (processedEventRepo.existsById(event.eventId())) {
			log.info("Skipping duplicate WTF event: eventId={}, type={}", event.eventId(), event.type());
			return;
		}

		log.info("Processing WTF event: type={}, eventId={}, targetUserId={}", event.type(), event.eventId(),
				event.targetUserId());

		try {
			recalculationService.applyEvent(event);

			// Record the event as processed (marks it idempotent for future redeliveries)
			processedEventRepo.save(new ProcessedEvent(event.eventId(), Instant.now()));

			log.info("Successfully processed WTF event: eventId={}", event.eventId());
		} catch (Exception e) {
			log.error("Failed to process WTF event: eventId={}, type={}", event.eventId(), event.type(), e);
			throw e; // Re-throw so RabbitMQ can retry or DLQ
		}
	}
}
