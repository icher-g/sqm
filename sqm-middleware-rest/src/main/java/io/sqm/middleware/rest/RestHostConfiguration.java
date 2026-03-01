package io.sqm.middleware.rest;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import java.time.Clock;

/**
 * Configures REST host security and abuse-protection behavior.
 */
@Configuration
@EnableConfigurationProperties({RestSecurityProperties.class, RestAbuseProtectionProperties.class})
public class RestHostConfiguration {

    /**
     * Creates REST host configuration.
     */
    public RestHostConfiguration() {
    }

    /**
     * Creates security filter chain that delegates endpoint auth to interceptors.
     *
     * @param http security builder
     * @return security filter chain
     * @throws Exception on security setup failure
     */
    @Bean
    public SecurityFilterChain restSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .httpBasic(httpBasic -> httpBasic.disable())
            .formLogin(form -> form.disable());
        return http.build();
    }

    /**
     * Provides API-key authentication interceptor.
     *
     * @param properties security properties
     * @return API-key authentication interceptor
     */
    @Bean
    public ApiKeyAuthInterceptor apiKeyAuthInterceptor(RestSecurityProperties properties) {
        return new ApiKeyAuthInterceptor(properties);
    }

    /**
     * Provides fixed-window limiter.
     *
     * @param properties abuse-protection properties
     * @return fixed-window limiter
     */
    @Bean
    public FixedWindowRateLimiter fixedWindowRateLimiter(RestAbuseProtectionProperties properties) {
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
        RestAbuseProtectionProperties properties,
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
    public FilterRegistrationBean<RequestSizeFilter> requestSizeFilterRegistration(RestAbuseProtectionProperties properties) {
        var registration = new FilterRegistrationBean<>(new RequestSizeFilter(properties));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.addUrlPatterns("/sqm/middleware/v1/*");
        return registration;
    }

    /**
     * Registers correlation-id propagation filter.
     *
     * @return filter registration
     */
    @Bean
    public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilterRegistration() {
        var registration = new FilterRegistrationBean<>(new CorrelationIdFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        registration.addUrlPatterns("/sqm/middleware/v1/*");
        return registration;
    }

}
