package com.sunday.common_lib.util;

/**
 * Cache name constants, shared across services. All cache-using services sit behind one
 * Redis instance/logical DB ({@code docker-compose.yml}'s {@code redis} container), so a
 * literal name reused by two services would silently share (and clobber) the same keyspace
 * under {@link org.springframework.data.redis.cache.RedisCacheManager}'s name-prefixed keys.
 * This class is the single source of truth every service's {@code @Cacheable}/{@code @CacheEvict}
 * annotations and {@code RedisConfig}'s TTL map must agree on.
 */
public final class CacheUtil {

    private CacheUtil() {}

    // ---------- airline-core-service ----------
    public static final String AIRLINES = "airlines";
    public static final String AIRLINES_BY_USER = "airlinesByUser";
    public static final String AIRLINES_DROPDOWN = "airlinesDropdown";
    public static final String AIRCRAFTS = "aircrafts";
    public static final String ROLE_PERMISSIONS = "rolePermissions";

    // ---------- pricing-service ----------
    public static final String FARES = "fares";

    // ---------- flight-ops-service ----------
    public static final String FLIGHT_INSTANCES = "flightInstances";

    // ---------- location-service ----------
    public static final String CITIES = "cities";
    public static final String AIRPORTS = "airports";
    public static final String AIRPORTS_BY_CITY = "airportsByCity";
}
