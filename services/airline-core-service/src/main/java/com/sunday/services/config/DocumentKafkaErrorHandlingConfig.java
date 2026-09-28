package com.sunday.services.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

/**
 * Retry then park: a failing document event is retried with growing pauses (1s, 2s, 4s, 8s, 16s), and if it still
 * fails it goes to {@code <topic>.DLT} for inspection and replay. Spring Boot applies a {@link CommonErrorHandler}
 * bean to its listener container factory automatically. Events that cannot even be deserialized are not retried.
 * The document itself stays PROCESSING; the sweeper republishes it later and eventually blocks it.
 */
@Configuration
public class DocumentKafkaErrorHandlingConfig {

    private static final int RETRIES = 5;

    @Bean
    CommonErrorHandler documentKafkaErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(RETRIES);
        backOff.setInitialInterval(1_000L);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(30_000L);
        return new DefaultErrorHandler(new DeadLetterPublishingRecoverer(kafkaTemplate), backOff);
    }
}
