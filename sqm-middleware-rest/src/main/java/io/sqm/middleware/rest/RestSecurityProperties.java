package io.sqm.middleware.rest;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Security properties for REST host authentication.
 */
@ConfigurationProperties(prefix = ConfigKeys.SECURITY_PREFIX)
public class RestSecurityProperties {

    private boolean apiKeyEnabled;
    private String apiKeyHeader = "X-API-Key";
    private List<String> apiKeys = new ArrayList<>();

    /**
     * Creates REST security properties holder.
     */
    public RestSecurityProperties() {
    }

    /**
     * Returns whether API-key authentication is enabled.
     *
     * @return {@code true} when API-key auth is active
     */
    public boolean isApiKeyEnabled() {
        return apiKeyEnabled;
    }

    /**
     * Sets whether API-key authentication is enabled.
     *
     * @param apiKeyEnabled api-key enabled flag
     */
    public void setApiKeyEnabled(boolean apiKeyEnabled) {
        this.apiKeyEnabled = apiKeyEnabled;
    }

    /**
     * Returns HTTP header used to carry API key.
     *
     * @return API key header
     */
    public String getApiKeyHeader() {
        return apiKeyHeader;
    }

    /**
     * Sets HTTP header used to carry API key.
     *
     * @param apiKeyHeader API key header
     */
    public void setApiKeyHeader(String apiKeyHeader) {
        this.apiKeyHeader = apiKeyHeader;
    }

    /**
     * Returns accepted API keys.
     *
     * @return accepted API keys
     */
    public List<String> getApiKeys() {
        return apiKeys;
    }

    /**
     * Sets accepted API keys.
     *
     * @param apiKeys accepted API keys
     */
    public void setApiKeys(List<String> apiKeys) {
        this.apiKeys = apiKeys == null ? new ArrayList<>() : new ArrayList<>(apiKeys);
    }
}
