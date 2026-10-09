package com.sunday.service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** A small dedicated pool for fanning the email+SMS sends of one event out in parallel, so the two
 *  external network calls overlap instead of serializing inside the Kafka listener thread. Separate
 *  from the common pool so a slow provider here can't starve unrelated async work elsewhere. */
@Configuration
public class NotificationExecutorConfig {

    @Bean
    public ExecutorService notificationDispatchExecutor() {
        return Executors.newFixedThreadPool(4);
    }
}
