package io.sqm.playground.rest.config;

import io.sqm.playground.rest.ratelimit.RateLimitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Objects;

/**
 * Registers playground MVC interceptors and CORS settings.
 */
@Configuration
public class PlaygroundWebMvcConfiguration implements WebMvcConfigurer {

    private final PlaygroundCorsProperties corsProperties;
    private final RateLimitInterceptor rateLimitInterceptor;

    /**
     * Creates MVC configuration for the playground host.
     *
     * @param corsProperties CORS properties
     * @param rateLimitInterceptor rate-limit interceptor
     */
    public PlaygroundWebMvcConfiguration(
        PlaygroundCorsProperties corsProperties,
        RateLimitInterceptor rateLimitInterceptor
    ) {
        this.corsProperties = Objects.requireNonNull(corsProperties, "corsProperties must not be null");
        this.rateLimitInterceptor = Objects.requireNonNull(rateLimitInterceptor, "rateLimitInterceptor must not be null");
    }

    /**
     * Registers rate-limit interceptor for playground endpoints.
     *
     * @param registry interceptor registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor).addPathPatterns("/api/v1/**");
    }

    /**
     * Registers CORS settings for playground endpoints.
     *
     * @param registry CORS registry
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        var mapping = registry.addMapping("/api/v1/**")
            .allowedMethods("GET", "POST", "OPTIONS");
        var allowedOrigins = corsProperties.getAllowedOrigins();
        if (!allowedOrigins.isEmpty()) {
            mapping.allowedOrigins(allowedOrigins.toArray(String[]::new));
        }
    }
}
