package io.sqm.middleware.rest;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Objects;

/**
 * Registers REST host MVC interceptors.
 */
@Configuration
public class RestWebMvcConfiguration implements WebMvcConfigurer {

    private final ApiKeyAuthInterceptor apiKeyAuthInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;

    /**
     * Creates MVC configuration with security and abuse interceptors.
     *
     * @param apiKeyAuthInterceptor API-key auth interceptor
     * @param rateLimitInterceptor rate-limit interceptor
     */
    public RestWebMvcConfiguration(ApiKeyAuthInterceptor apiKeyAuthInterceptor, RateLimitInterceptor rateLimitInterceptor) {
        this.apiKeyAuthInterceptor = Objects.requireNonNull(apiKeyAuthInterceptor, "apiKeyAuthInterceptor must not be null");
        this.rateLimitInterceptor = Objects.requireNonNull(rateLimitInterceptor, "rateLimitInterceptor must not be null");
    }

    /**
     * Registers API-key and rate-limit interceptors for middleware endpoints.
     *
     * @param registry interceptor registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiKeyAuthInterceptor).addPathPatterns("/sqm/middleware/**");
        registry.addInterceptor(rateLimitInterceptor).addPathPatterns("/sqm/middleware/**");
    }
}
