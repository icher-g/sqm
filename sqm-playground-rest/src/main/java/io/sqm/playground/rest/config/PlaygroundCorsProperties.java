package io.sqm.playground.rest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * CORS properties for the playground REST host.
 */
@ConfigurationProperties(prefix = "sqm.playground.rest.cors")
public class PlaygroundCorsProperties {

    private List<String> allowedOrigins = new ArrayList<>(List.of("http://localhost:5173"));

    /**
     * Creates CORS properties holder.
     */
    public PlaygroundCorsProperties() {
    }

    /**
     * Returns allowed frontend origins.
     *
     * @return allowed origins
     */
    public List<String> getAllowedOrigins() {
        return List.copyOf(allowedOrigins);
    }

    /**
     * Sets allowed frontend origins.
     *
     * @param allowedOrigins allowed origins
     */
    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins == null ? new ArrayList<>() : new ArrayList<>(allowedOrigins);
    }
}
