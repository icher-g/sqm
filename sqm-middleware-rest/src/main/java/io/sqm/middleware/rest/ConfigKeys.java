package io.sqm.middleware.rest;

/**
 * Property keys used by the REST host module.
 */
public final class ConfigKeys {

    /**
     * Common REST configuration prefix.
     */
    public static final String REST_PREFIX = "sqm.middleware.rest";

    /**
     * Security configuration prefix.
     */
    public static final String SECURITY_PREFIX = REST_PREFIX + ".security";

    /**
     * Abuse-protection configuration prefix.
     */
    public static final String ABUSE_PREFIX = REST_PREFIX + ".abuse";

    /**
     * Enables API-key authentication for REST endpoints.
     */
    public static final String SECURITY_API_KEY_ENABLED = SECURITY_PREFIX + ".apiKeyEnabled";

    /**
     * API key header name.
     */
    public static final String SECURITY_API_KEY_HEADER = SECURITY_PREFIX + ".apiKeyHeader";

    /**
     * Comma-separated list of accepted API keys.
     */
    public static final String SECURITY_API_KEYS = SECURITY_PREFIX + ".apiKeys";

    /**
     * Enables rate limiting for REST endpoints.
     */
    public static final String ABUSE_RATE_LIMIT_ENABLED = ABUSE_PREFIX + ".rateLimitEnabled";

    /**
     * Maximum requests allowed per fixed window.
     */
    public static final String ABUSE_RATE_LIMIT_REQUESTS_PER_WINDOW = ABUSE_PREFIX + ".requestsPerWindow";

    /**
     * Fixed window size in seconds for rate limiting.
     */
    public static final String ABUSE_RATE_LIMIT_WINDOW_SECONDS = ABUSE_PREFIX + ".windowSeconds";

    /**
     * Maximum accepted HTTP request size in bytes.
     */
    public static final String ABUSE_MAX_REQUEST_BYTES = ABUSE_PREFIX + ".maxRequestBytes";

    /**
     * Enables trusted proxy header resolution for rate-limiting client identity.
     */
    public static final String ABUSE_TRUST_PROXY_HEADERS = ABUSE_PREFIX + ".trustProxyHeaders";

    /**
     * Header name used for extracting client IP when proxy headers are trusted.
     */
    public static final String ABUSE_CLIENT_IP_HEADER = ABUSE_PREFIX + ".clientIpHeader";

    private ConfigKeys() {
    }
}
