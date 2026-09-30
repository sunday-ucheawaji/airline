package com.sunday.services.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

/**
 * Retry + dead-letter-topic policy for the flight-instance-created / flight-instance-aircraft-changed
 * listeners in {@link com.sunday.services.event.FlightInstanceEventConsumer} — mirrors
 * airline-core-service's DocumentKafkaErrorHandlingConfig. Previously this consumer had no error
 * handling at all: a transient failure (e.g. a DB hiccup) would silently commit the offset and drop
 * the event.
 */
@Configuration
public class FlightOpsKafkaErrorHandlingConfig {

    private static final int RETRIES = 5;

    @Bean
    public CommonErrorHandler flightOpsKafkaErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(RETRIES);
        backOff.setInitialInterval(1_000L);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(30_000L);
        return new DefaultErrorHandler(new DeadLetterPublishingRecoverer(kafkaTemplate), backOff);
    }
}
