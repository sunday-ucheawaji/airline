package com.sunday.service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

/**
 * Retry then park: a failing notification event is retried with growing pauses (1s, 2s, 4s, 8s,
 * 16s), and if it still fails it goes to <topic>.DLT for inspection and replay — same pattern as
 * DocumentKafkaErrorHandlingConfig (airline-core-service) / FlightOpsKafkaErrorHandlingConfig
 * (seat-service) / PaymentKafkaErrorHandlingConfig (booking-service). This is the one notification-
 * service never had: its old FixedBackOff(5s, 2 retries) had no recoverer at all, and in any case the
 * listener used to catch every send exception itself, so this error handler was never even reached.
 */
@Configuration
public class NotificationKafkaErrorHandlingConfig {

    private static final int RETRIES = 5;

    @Bean
    public CommonErrorHandler notificationKafkaErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(RETRIES);
        backOff.setInitialInterval(1_000L);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(30_000L);
        return new DefaultErrorHandler(new DeadLetterPublishingRecoverer(kafkaTemplate), backOff);
    }
}
