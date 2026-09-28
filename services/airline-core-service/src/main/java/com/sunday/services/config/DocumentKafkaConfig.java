package com.sunday.services.config;

import com.sunday.common_lib.event.OnboardingDocumentUploadedEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/** Topics for the asynchronous document processing. Producer/consumer settings live in {@code spring.kafka.*}. */
@Configuration
public class DocumentKafkaConfig {

    private static final int PARTITIONS = 3;
    private static final String DEAD_LETTER_SUFFIX = ".DLT";

    @Bean
    NewTopic onboardingDocumentUploadedTopic() {
        return TopicBuilder.name(OnboardingDocumentUploadedEvent.TOPIC).partitions(PARTITIONS).replicas(1).build();
    }

    /** Events that keep failing after their retries are parked here for inspection and replay. */
    @Bean
    NewTopic onboardingDocumentUploadedDeadLetterTopic() {
        return TopicBuilder.name(OnboardingDocumentUploadedEvent.TOPIC + DEAD_LETTER_SUFFIX).partitions(PARTITIONS).replicas(1).build();
    }
}
