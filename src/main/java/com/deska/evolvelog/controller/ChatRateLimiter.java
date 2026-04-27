package com.deska.evolvelog.controller;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory per-user rate limiter for the AI chat endpoint.
 *
 * <p>Enforces two limits:
 * <ul>
 *   <li>At most {@value #MAX_CONCURRENT} active streaming request per user at a time.</li>
 *   <li>At most {@value #MAX_PER_HOUR} requests per user per rolling hour.</li>
 * </ul>
 *
 * <p>No external dependencies — state is JVM-local and resets on restart.
 *
 * <p><strong>Scaling note:</strong> This implementation is per-JVM. In a multi-instance deployment
 * each pod tracks limits independently, so effective limits are multiplied by instance count.
 * Migrate to a Redis-backed implementation (e.g. Bucket4j + Redis, or plain {@code INCR}/{@code ZADD})
 * before horizontal scaling.
 */
@Component
public class ChatRateLimiter {

    static final int MAX_CONCURRENT = 1;
    static final int MAX_PER_HOUR = 30;
    private static final long WINDOW_MILLIS = 3_600_000L;

    private record UserState(AtomicInteger active, Deque<Instant> timestamps) {}

    private final ConcurrentHashMap<UUID, UserState> state = new ConcurrentHashMap<>();

    /**
     * Attempts to acquire a chat slot for the given user.
     *
     * @return {@code true} if the request is allowed, {@code false} if rate-limited
     */
    public boolean tryAcquire(UUID userId) {
        UserState us = state.computeIfAbsent(userId,
                k -> new UserState(new AtomicInteger(0), new ArrayDeque<>()));

        synchronized (us) {
            evictOldTimestamps(us.timestamps());
            if (us.timestamps().size() >= MAX_PER_HOUR) return false;
            if (us.active().get() >= MAX_CONCURRENT) return false;
            us.active().incrementAndGet();
            us.timestamps().addLast(Instant.now());
            return true;
        }
    }

    /**
     * Releases the active slot for the given user. Must be called when the stream ends
     * (completion, timeout, or error).
     */
    public void release(UUID userId) {
        UserState us = state.get(userId);
        if (us != null) {
            synchronized (us) {
                us.active().decrementAndGet();
            }
        }
    }

    private void evictOldTimestamps(Deque<Instant> timestamps) {
        Instant cutoff = Instant.now().minusMillis(WINDOW_MILLIS);
        while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(cutoff)) {
            timestamps.pollFirst();
        }
    }
}
