package com.flashexchange.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration 
public class RabbitMQConfig {
    
    public static final String EXCHANGE_NAME = "flashsale.direct.exchange";
    public static final String QUEUE_NAME = "flashsale.order.queue";
    public static final String ROUTING_KEY = "flashsale.order.key";

    public static final String DELAY_QUEUE_NAME = "flashsale.delay.queue";
    public static final String DELAY_ROUTING_KEY = "flashsale.delay.key";

    public static final String CANCEL_QUEUE_NAME = "flashsale.cancel.queue";
    public static final String CANCEL_ROUTING_KEY = "flashsale.cancel.key";

    @Bean 
    public Queue delayQueue() {
        return org.springframework.amqp.core.QueueBuilder.durable(DELAY_QUEUE_NAME)
               .ttl(10000)
               .deadLetterExchange(EXCHANGE_NAME)
               .deadLetterRoutingKey(CANCEL_ROUTING_KEY)
               .build();
    }
    
    @Bean 
    public Queue cancleQueue() {
        return new Queue(CANCEL_QUEUE_NAME, true);
    }

    @Bean 
    public Binding delayBinding(Queue delayQueue, DirectExchange flashSaleExchange) {
        return BindingBuilder.bind(delayQueue).to(flashSaleExchange).with(DELAY_ROUTING_KEY);
    }

    @Bean
    public Binding cancelBinding(Queue cancleQueue, DirectExchange flashSaleExchange) {
        return BindingBuilder.bind(cancleQueue).to(flashSaleExchange).with(CANCEL_ROUTING_KEY);
    }

    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue flashSaleQueue() {
        return new Queue(QUEUE_NAME, true);
    }

    @Bean 
    public Binding flashSaleBinding(Queue flashSaleQueue, DirectExchange flashSaleExchange) {
        return BindingBuilder.bind(flashSaleQueue).to(flashSaleExchange).with(ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
