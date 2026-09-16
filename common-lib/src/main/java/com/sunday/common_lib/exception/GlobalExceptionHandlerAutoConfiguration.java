package com.sunday.common_lib.exception;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Makes {@link GlobalExceptionHandler} available to every service that depends
 * on common-lib without each service having to widen its component scan.
 *
 * The handler lives in {@code com.sunday.common_lib.exception}, which is a sibling
 * of the service scan roots ({@code com.sunday.services} / {@code com.sunday.service}
 * / {@code com.sunday.cloud}), so it would otherwise never be picked up.
 *
 * Registered through
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}.
 * Guards keep it inert for non-web modules (e.g. config-server, service-registry)
 * and let a service override it with its own advice bean if needed.
 */
@AutoConfiguration
@ConditionalOnClass(RestControllerAdvice.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class GlobalExceptionHandlerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }
}
