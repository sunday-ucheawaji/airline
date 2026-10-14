package com.sunday.services.config;

import java.util.UUID;
import com.sunday.common_lib.util.CacheUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

/** Evicts a cached fare when one of its children (rules, baggage policy) changes. Deferred to after commit. */
@Component
@RequiredArgsConstructor
public class FareCacheEvictor {

    private final CacheManager cacheManager;

    public void evict(UUID fareId) {
        Cache cache = cacheManager.getCache(CacheUtil.FARES);
        if (cache != null) {
            cache.evict(fareId);
        }
    }
}
