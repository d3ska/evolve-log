package com.deska.evolvelog.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ChatRateLimiterTest {

    private ChatRateLimiter rateLimiter;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        rateLimiter = new ChatRateLimiter();
    }

    @Test
    void shouldAllowFirstRequest() {
        // when
        boolean acquired = rateLimiter.tryAcquire(userId);

        // then
        assertThat(acquired).isTrue();
    }

    @Test
    void shouldDenySecondConcurrentRequest() {
        // given
        rateLimiter.tryAcquire(userId);

        // when
        boolean second = rateLimiter.tryAcquire(userId);

        // then
        assertThat(second).isFalse();
    }

    @Test
    void shouldAllowNewRequestAfterRelease() {
        // given
        rateLimiter.tryAcquire(userId);
        rateLimiter.release(userId);

        // when
        boolean acquired = rateLimiter.tryAcquire(userId);

        // then
        assertThat(acquired).isTrue();
    }

    @Test
    void shouldIsolateLimitsBetweenDifferentUsers() {
        // given
        UUID otherUser = UUID.randomUUID();
        rateLimiter.tryAcquire(userId);

        // when
        boolean otherAcquired = rateLimiter.tryAcquire(otherUser);

        // then
        assertThat(otherAcquired).isTrue();
    }

    @Test
    void shouldDenyRequestWhenHourlyLimitReached() {
        // given — exhaust hourly limit without holding concurrent slot
        for (int i = 0; i < ChatRateLimiter.MAX_PER_HOUR; i++) {
            assertThat(rateLimiter.tryAcquire(userId)).isTrue();
            rateLimiter.release(userId);
        }

        // when
        boolean overLimit = rateLimiter.tryAcquire(userId);

        // then
        assertThat(overLimit).isFalse();
    }

    @Test
    void shouldNotThrowOnReleaseWithNoActiveRequest() {
        // when / then — no exception
        rateLimiter.release(userId);
        rateLimiter.release(UUID.randomUUID());
    }
}
