package com.sunday.services.config;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

/** Evicts a cached fare when one of its children (rules, baggage policy) changes. */
@Component
@RequiredArgsConstructor
public class FareCacheEvictor {

    private final CacheManager cacheManager;

    public void evict(Long fareId) {
        Cache cache = cacheManager.getCache(RedisConfig.FARES);
        if (cache != null) {
            cache.evictIfPresent(fareId);
        }
    }
}
