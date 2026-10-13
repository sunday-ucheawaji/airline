package com.sunday.common_lib.lock;

import com.sunday.common_lib.exception.ConflictException;
import com.sunday.common_lib.exception.ServiceUnavailableException;
import com.sunday.common_lib.util.ErrorMessageUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Serializes changes to whether a user is platform staff or an airline member. user-service (role grants) and
 * airline-core-service (invitations, provisioning) each check the other side before writing; holding this lock,
 * keyed by user id in the shared Redis, until their transaction ends makes those checks and writes atomic.
 */
public class UserAccessLock {

    private static final String KEY_PREFIX = "lock:user-access:";
    private static final DefaultRedisScript<Long> RELEASE = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end", Long.class);

    private final StringRedisTemplate redis;
    private final Duration leaseTime;
    private final Duration waitTime;
    private final Duration retryInterval;

    public UserAccessLock(StringRedisTemplate redis) {
        this(redis, Duration.ofSeconds(30), Duration.ofSeconds(3), Duration.ofMillis(100));
    }

    public UserAccessLock(StringRedisTemplate redis, Duration leaseTime, Duration waitTime, Duration retryInterval) {
        this.redis = redis;
        this.leaseTime = leaseTime;
        this.waitTime = waitTime;
        this.retryInterval = retryInterval;
    }

    /** Takes the lock for {@code userId} and releases it once the current transaction commits or rolls back. */
    public void lockForCurrentTransaction(UUID userId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("UserAccessLock must be taken inside a transaction");
        }
        String key = KEY_PREFIX + userId;
        String token = UUID.randomUUID().toString();
        acquire(key, token, userId);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                release(key, token);
            }
        });
    }

    private void acquire(String key, String token, UUID userId) {
        long deadline = System.nanoTime() + waitTime.toNanos();
        while (true) {
            Boolean acquired;
            try {
                acquired = redis.opsForValue().setIfAbsent(key, token, leaseTime);
            } catch (RuntimeException e) {
                throw new ServiceUnavailableException(String.format(ErrorMessageUtil.USER_ACCESS_LOCK_UNAVAILABLE, userId), e);
            }
            if (Boolean.TRUE.equals(acquired)) {
                return;
            }
            if (System.nanoTime() >= deadline) {
                throw new ConflictException(String.format(ErrorMessageUtil.USER_ACCESS_LOCK_BUSY, userId));
            }
            try {
                Thread.sleep(retryInterval.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ServiceUnavailableException(String.format(ErrorMessageUtil.USER_ACCESS_LOCK_UNAVAILABLE, userId), e);
            }
        }
    }

    private void release(String key, String token) {
        try {
            redis.execute(RELEASE, List.of(key), token);
        } catch (RuntimeException ignored) {
            // The lease expires on its own.
        }
    }
}
