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
    private boolean trustProxyHeaders;
    private String clientIpHeader = "X-Forwarded-For";

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

    /**
     * Returns whether trusted proxy headers are used to resolve client IP for rate limiting.
     *
     * @return {@code true} when proxy headers are trusted
     */
    public boolean isTrustProxyHeaders() {
        return trustProxyHeaders;
    }

    /**
     * Sets whether trusted proxy headers are used to resolve client IP for rate limiting.
     *
     * @param trustProxyHeaders trust proxy headers flag
     */
    public void setTrustProxyHeaders(boolean trustProxyHeaders) {
        this.trustProxyHeaders = trustProxyHeaders;
    }

    /**
     * Returns header name used to resolve client IP when proxy headers are trusted.
     *
     * @return client IP header name
     */
    public String getClientIpHeader() {
        return clientIpHeader;
    }

    /**
     * Sets header name used to resolve client IP when proxy headers are trusted.
     *
     * @param clientIpHeader client IP header name
     */
    public void setClientIpHeader(String clientIpHeader) {
        this.clientIpHeader = clientIpHeader;
    }
}
