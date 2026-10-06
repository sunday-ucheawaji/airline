package com.sunday.services.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ClockConfig {

    /** UTC clock for sales windows and advance purchase. Tests substitute a fixed clock. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
