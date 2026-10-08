package com.sunday.services.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

/**
 * Retry then park: a failing payment.completed/payment.failed delivery is retried with growing
 * pauses (1s, 2s, 4s, 8s, 16s), and if it still fails it goes to <topic>.DLT for inspection and
 * replay, mirroring DocumentKafkaErrorHandlingConfig / FlightOpsKafkaErrorHandlingConfig.
 */
@Configuration
public class PaymentKafkaErrorHandlingConfig {

    private static final int RETRIES = 5;

    @Bean
    public CommonErrorHandler paymentKafkaErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(RETRIES);
        backOff.setInitialInterval(1_000L);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(30_000L);
        return new DefaultErrorHandler(new DeadLetterPublishingRecoverer(kafkaTemplate), backOff);
    }
}
