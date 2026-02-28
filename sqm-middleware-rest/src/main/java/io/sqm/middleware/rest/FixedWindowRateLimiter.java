package io.sqm.middleware.rest;

import java.time.Clock;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fixed-window per-key rate limiter.
 */
public final class FixedWindowRateLimiter {

    private final int requestsPerWindow;
    private final long windowMillis;
    private final Clock clock;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    /**
     * Creates fixed-window limiter.
     *
     * @param requestsPerWindow max requests per window
     * @param windowSeconds window length in seconds
     * @param clock time source
     */
    public FixedWindowRateLimiter(int requestsPerWindow, int windowSeconds, Clock clock) {
        if (requestsPerWindow <= 0) {
            throw new IllegalArgumentException("requestsPerWindow must be > 0");
        }
        if (windowSeconds <= 0) {
            throw new IllegalArgumentException("windowSeconds must be > 0");
        }
        this.requestsPerWindow = requestsPerWindow;
        this.windowMillis = windowSeconds * 1000L;
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * Attempts to acquire one request slot for the key.
     *
     * @param key limiter key
     * @return {@code true} when allowed
     */
    public boolean allow(String key) {
        var limiterKey = key == null || key.isBlank() ? "unknown" : key;
        var now = clock.millis();
        var windowStart = (now / windowMillis) * windowMillis;
        var window = windows.compute(limiterKey, (k, previous) -> {
            if (previous == null || previous.startMillis != windowStart) {
                return new Window(windowStart, new AtomicInteger(0));
            }
            return previous;
        });
        return window.count.incrementAndGet() <= requestsPerWindow;
    }

    private record Window(long startMillis, AtomicInteger count) {
    }
}
