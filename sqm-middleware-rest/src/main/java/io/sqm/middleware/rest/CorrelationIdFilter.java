package io.sqm.middleware.rest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Adds request correlation id to response headers and logging MDC.
 */
public final class CorrelationIdFilter extends OncePerRequestFilter {

    /**
     * Header used for inbound/outbound correlation id propagation.
     */
    public static final String HEADER_NAME = "X-Correlation-Id";

    /**
     * MDC key used for correlation id in logs.
     */
    public static final String MDC_KEY = "correlationId";

    /**
     * Creates correlation-id propagation filter.
     */
    public CorrelationIdFilter() {
    }

    /**
     * Populates correlation id from request header or generates one when missing.
     *
     * @param request HTTP request
     * @param response HTTP response
     * @param filterChain filter chain
     * @throws ServletException on filter errors
     * @throws IOException on I/O errors
     */
    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        var incoming = request.getHeader(HEADER_NAME);
        var correlationId = (incoming == null || incoming.isBlank()) ? UUID.randomUUID().toString() : incoming.trim();
        request.setAttribute(HEADER_NAME, correlationId);
        response.setHeader(HEADER_NAME, correlationId);
        MDC.put(MDC_KEY, correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
