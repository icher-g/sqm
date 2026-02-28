package io.sqm.middleware.rest;

import jakarta.annotation.Nonnull;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Enforces API-key authentication for REST endpoints when enabled.
 */
public final class ApiKeyAuthInterceptor implements HandlerInterceptor {

    private final RestSecurityProperties properties;

    /**
     * Creates interceptor backed by REST security properties.
     *
     * @param properties security properties
     */
    public ApiKeyAuthInterceptor(RestSecurityProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    /**
     * Validates API key before request reaches controller.
     *
     * @param request HTTP request
     * @param response HTTP response
     * @param handler selected handler
     * @return {@code true} when request is authorized
     */
    @Override
    public boolean preHandle(@Nonnull HttpServletRequest request, @Nonnull HttpServletResponse response, @Nonnull Object handler) {
        if (!properties.isApiKeyEnabled()) {
            return true;
        }

        var header = properties.getApiKeyHeader();
        if (header == null || header.isBlank()) {
            throw new UnauthorizedRequestException("API key header is not configured");
        }

        var expectedKeys = properties.getApiKeys().stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(v -> !v.isEmpty())
            .collect(Collectors.toUnmodifiableSet());

        if (expectedKeys.isEmpty()) {
            throw new UnauthorizedRequestException("No API keys are configured");
        }

        var provided = request.getHeader(header);
        if (provided == null || provided.isBlank() || !expectedKeys.contains(provided)) {
            throw new UnauthorizedRequestException("Invalid or missing API key");
        }
        return true;
    }
}
