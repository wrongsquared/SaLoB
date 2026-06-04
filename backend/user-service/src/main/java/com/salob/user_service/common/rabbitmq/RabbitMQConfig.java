package com.salob.user_service.common.rabbitmq;

import com.salob.proto.common.RabbitMQConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@EnableRabbit
public class RabbitMQConfig {

	@Bean
	public Jackson2JsonMessageConverter jsonMessageConverter() {
		return new Jackson2JsonMessageConverter();
	}

	@Bean
	public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, Jackson2JsonMessageConverter converter) {
		RabbitTemplate template = new RabbitTemplate(connectionFactory);
		template.setMessageConverter(converter);
		return template;
	}

	@Bean
	public TopicExchange eventsExchange() {
		return new TopicExchange(RabbitMQConstants.EVENTS_EXCHANGE);
	}

	@Bean
	public Queue wtfEntryCreatedQueue() {
		return new Queue(RabbitMQConstants.QUEUE_USER_WTF_ENTRY_CREATED, true, false, false,
				Map.of("x-dead-letter-exchange", "salob.dlx", "x-dead-letter-routing-key", "wtf.entry.created.dead"));
	}

	@Bean
	public Queue wtfVoteCastQueue() {
		return new Queue(RabbitMQConstants.QUEUE_USER_WTF_VOTE_CAST, true, false, false,
				Map.of("x-dead-letter-exchange", "salob.dlx", "x-dead-letter-routing-key", "wtf.vote.cast.dead"));
	}

	@Bean
	public Queue wtfFlagRaisedQueue() {
		return new Queue(RabbitMQConstants.QUEUE_USER_WTF_FLAG_RAISED, true, false, false,
				Map.of("x-dead-letter-exchange", "salob.dlx", "x-dead-letter-routing-key", "wtf.flag.raised.dead"));
	}

	@Bean
	public Binding wtfEntryCreatedBinding(TopicExchange eventsExchange, Queue wtfEntryCreatedQueue) {
		return BindingBuilder.bind(wtfEntryCreatedQueue).to(eventsExchange)
				.with(RabbitMQConstants.RK_WTF_ENTRY_CREATED);
	}

	@Bean
	public Binding wtfVoteCastBinding(TopicExchange eventsExchange, Queue wtfVoteCastQueue) {
		return BindingBuilder.bind(wtfVoteCastQueue).to(eventsExchange).with(RabbitMQConstants.RK_WTF_VOTE_CAST);
	}

	@Bean
	public Binding wtfFlagRaisedBinding(TopicExchange eventsExchange, Queue wtfFlagRaisedQueue) {
		return BindingBuilder.bind(wtfFlagRaisedQueue).to(eventsExchange).with(RabbitMQConstants.RK_WTF_FLAG_RAISED);
	}
}
