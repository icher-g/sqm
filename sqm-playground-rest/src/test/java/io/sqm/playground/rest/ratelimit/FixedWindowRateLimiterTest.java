package io.sqm.playground.rest.ratelimit;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests fixed-window rate limiter behavior.
 */
class FixedWindowRateLimiterTest {

    @Test
    void constructorRejectsNonPositiveSettings() {
        var clock = new MutableClock();

        assertThrows(IllegalArgumentException.class, () -> new FixedWindowRateLimiter(0, 1, clock));
        assertThrows(IllegalArgumentException.class, () -> new FixedWindowRateLimiter(1, 0, clock));
    }

    @Test
    void blankAndNullKeysShareUnknownBucket() {
        var limiter = new FixedWindowRateLimiter(1, 60, new MutableClock());

        assertTrue(limiter.allow(" "));
        assertFalse(limiter.allow(null));
    }

    @Test
    void movingToNextWindowResetsAllowance() {
        var clock = new MutableClock();
        var limiter = new FixedWindowRateLimiter(1, 60, clock);

        assertTrue(limiter.allow("client-a"));
        assertFalse(limiter.allow("client-a"));

        clock.setMillis(60_000L);

        assertTrue(limiter.allow("client-a"));
    }

    @Test
    void differentKeysKeepIndependentCounters() {
        var limiter = new FixedWindowRateLimiter(1, 60, new MutableClock());

        assertTrue(limiter.allow("client-a"));
        assertTrue(limiter.allow("client-b"));
        assertFalse(limiter.allow("client-a"));
    }

    private static final class MutableClock extends Clock {
        private long millis;

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(millis);
        }

        void setMillis(long millis) {
            this.millis = millis;
        }
    }
}
