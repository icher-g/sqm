package io.sqm.playground.rest.config;

import io.sqm.playground.rest.PlaygroundApiPaths;
import io.sqm.playground.rest.filter.RequestSizeFilter;
import io.sqm.playground.rest.ratelimit.FixedWindowRateLimiter;
import io.sqm.playground.rest.ratelimit.RateLimitInterceptor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import java.time.Clock;

/**
 * Configures playground host abuse-protection behavior.
 */
@Configuration
@EnableConfigurationProperties({PlaygroundAbuseProtectionProperties.class, PlaygroundCorsProperties.class})
public class PlaygroundHostConfiguration {

    /**
     * Creates playground host configuration.
     */
    public PlaygroundHostConfiguration() {
    }

    /**
     * Provides fixed-window limiter.
     *
     * @param properties abuse-protection properties
     * @return fixed-window limiter
     */
    @Bean
    public FixedWindowRateLimiter fixedWindowRateLimiter(PlaygroundAbuseProtectionProperties properties) {
        return new FixedWindowRateLimiter(
            Math.max(1, properties.getRequestsPerWindow()),
            Math.max(1, properties.getWindowSeconds()),
            Clock.systemUTC()
        );
    }

    /**
     * Provides rate-limit interceptor.
     *
     * @param properties abuse-protection properties
     * @param limiter fixed-window limiter
     * @return rate-limit interceptor
     */
    @Bean
    public RateLimitInterceptor rateLimitInterceptor(
        PlaygroundAbuseProtectionProperties properties,
        FixedWindowRateLimiter limiter
    ) {
        return new RateLimitInterceptor(properties, limiter);
    }

    /**
     * Registers request-size filter.
     *
     * @param properties abuse-protection properties
     * @return filter registration
     */
    @Bean
    public FilterRegistrationBean<RequestSizeFilter> requestSizeFilterRegistration(PlaygroundAbuseProtectionProperties properties) {
        var registration = new FilterRegistrationBean<>(new RequestSizeFilter(properties));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.addUrlPatterns(PlaygroundApiPaths.FILTER_PATTERN);
        return registration;
    }
}
