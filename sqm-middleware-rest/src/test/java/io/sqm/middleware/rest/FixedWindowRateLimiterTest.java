package io.sqm.middleware.rest;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class FixedWindowRateLimiterTest {

    @Test
    void validates_constructor_arguments() {
        var clock = Clock.systemUTC();
        assertThrows(IllegalArgumentException.class, () -> new FixedWindowRateLimiter(0, 1, clock));
        assertThrows(IllegalArgumentException.class, () -> new FixedWindowRateLimiter(1, 0, clock));
        assertThrows(NullPointerException.class, () -> new FixedWindowRateLimiter(1, 1, null));
    }

    @Test
    void allows_until_limit_then_rejects_in_same_window() {
        var epochMillis = new AtomicLong(1_000);
        var tickingClock = new Clock() {
            @Override
            public ZoneOffset getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(java.time.ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return Instant.ofEpochMilli(epochMillis.get());
            }
        };

        var limiter = new FixedWindowRateLimiter(2, 60, tickingClock);
        assertTrue(limiter.allow("user"));
        assertTrue(limiter.allow("user"));
        assertFalse(limiter.allow("user"));

        epochMillis.addAndGet(60_000);
        assertTrue(limiter.allow("user"));
    }

    @Test
    void treats_blank_key_as_unknown_bucket() {
        var limiter = new FixedWindowRateLimiter(1, 60, Clock.systemUTC());
        assertTrue(limiter.allow(" "));
        assertFalse(limiter.allow(null));
    }

    @Test
    void evicts_stale_key_windows_during_periodic_cleanup() {
        var epochMillis = new AtomicLong(1_000);
        var tickingClock = new Clock() {
            @Override
            public ZoneOffset getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(java.time.ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return Instant.ofEpochMilli(epochMillis.get());
            }
        };

        var limiter = new FixedWindowRateLimiter(10, 1, tickingClock);

        // Create many distinct key windows in the first second bucket.
        for (int i = 0; i < 260; i++) {
            assertTrue(limiter.allow("key-" + i));
        }
        assertTrue(limiter.trackedKeyCount() >= 256);

        // Advance beyond retained window horizon and trigger periodic eviction.
        epochMillis.addAndGet(5_000);
        for (int i = 0; i < 260; i++) {
            assertTrue(limiter.allow("new-" + i));
        }

        assertTrue(limiter.trackedKeyCount() < 520);
    }
}
