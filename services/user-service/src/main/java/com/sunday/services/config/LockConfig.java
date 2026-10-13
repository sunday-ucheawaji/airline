package com.sunday.services.config;

import com.sunday.common_lib.lock.UserAccessLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class LockConfig {

    @Bean
    UserAccessLock userAccessLock(StringRedisTemplate redis) {
        return new UserAccessLock(redis);
    }
}
