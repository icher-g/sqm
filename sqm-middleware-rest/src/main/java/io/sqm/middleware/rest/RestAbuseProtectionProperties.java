package io.sqm.middleware.rest;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Abuse-protection properties for REST host endpoints.
 */
@ConfigurationProperties(prefix = ConfigKeys.ABUSE_PREFIX)
public class RestAbuseProtectionProperties {

    private boolean rateLimitEnabled;
    private int requestsPerWindow = 60;
    private int windowSeconds = 60;
    private long maxRequestBytes = 64 * 1024;

    /**
     * Returns whether rate limiting is enabled.
     *
     * @return {@code true} when enabled
     */
    public boolean isRateLimitEnabled() {
        return rateLimitEnabled;
    }

    /**
     * Sets whether rate limiting is enabled.
     *
     * @param rateLimitEnabled enabled flag
     */
    public void setRateLimitEnabled(boolean rateLimitEnabled) {
        this.rateLimitEnabled = rateLimitEnabled;
    }

    /**
     * Returns maximum requests allowed per window.
     *
     * @return requests per window
     */
    public int getRequestsPerWindow() {
        return requestsPerWindow;
    }

    /**
     * Sets maximum requests allowed per window.
     *
     * @param requestsPerWindow requests per window
     */
    public void setRequestsPerWindow(int requestsPerWindow) {
        this.requestsPerWindow = requestsPerWindow;
    }

    /**
     * Returns window size in seconds.
     *
     * @return window size in seconds
     */
    public int getWindowSeconds() {
        return windowSeconds;
    }

    /**
     * Sets window size in seconds.
     *
     * @param windowSeconds window size in seconds
     */
    public void setWindowSeconds(int windowSeconds) {
        this.windowSeconds = windowSeconds;
    }

    /**
     * Returns maximum accepted request body size in bytes.
     *
     * @return max request bytes
     */
    public long getMaxRequestBytes() {
        return maxRequestBytes;
    }

    /**
     * Sets maximum accepted request body size in bytes.
     *
     * @param maxRequestBytes max request bytes
     */
    public void setMaxRequestBytes(long maxRequestBytes) {
        this.maxRequestBytes = maxRequestBytes;
    }
}
