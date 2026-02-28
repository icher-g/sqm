package io.sqm.middleware.rest;

import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Rejects requests whose declared content length exceeds configured limit.
 */
public final class RequestSizeFilter extends OncePerRequestFilter {

    private final RestAbuseProtectionProperties properties;

    /**
     * Creates request-size filter.
     *
     * @param properties abuse-protection properties
     */
    public RequestSizeFilter(RestAbuseProtectionProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    private static byte[] readAndValidateBody(HttpServletRequest request, long maxBytes) throws IOException {
        try (var input = request.getInputStream()) {
            var limit = maxBytes + 1;
            var buffer = input.readNBytes((int) Math.min(limit, Integer.MAX_VALUE));
            if (buffer.length > maxBytes) {
                throw new RequestTooLargeException("Request body exceeds max allowed size");
            }
            return buffer;
        }
    }

    private static void writePayloadTooLarge(HttpServletResponse response, String path, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        var safeMessage = message == null ? "" : message.replace("\"", "\\\"");
        var safePath = path == null ? "" : path.replace("\"", "\\\"");
        response.getWriter().write(
            "{\"code\":\"REQUEST_TOO_LARGE\",\"message\":\"" + safeMessage + "\",\"path\":\"" + safePath + "\"}"
        );
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, @Nonnull HttpServletResponse response, @Nonnull FilterChain filterChain) throws ServletException, IOException {
        try {
            var maxBytes = properties.getMaxRequestBytes();
            var contentLength = request.getContentLengthLong();
            if (maxBytes <= 0) {
                filterChain.doFilter(request, response);
                return;
            }
            if (contentLength > maxBytes) {
                throw new RequestTooLargeException("Request body exceeds max allowed size");
            }
            if (contentLength >= 0) {
                filterChain.doFilter(request, response);
                return;
            }

            var cachedBody = readAndValidateBody(request, maxBytes);
            filterChain.doFilter(new CachedBodyRequest(request, cachedBody), response);
        } catch (RequestTooLargeException ex) {
            writePayloadTooLarge(response, request.getRequestURI(), ex.getMessage());
        }
    }

    private static final class CachedBodyRequest extends HttpServletRequestWrapper {

        private final byte[] body;

        private CachedBodyRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            return new CachedBodyServletInputStream(body);
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }

    private static final class CachedBodyServletInputStream extends ServletInputStream {

        private final ByteArrayInputStream delegate;

        private CachedBodyServletInputStream(byte[] body) {
            this.delegate = new ByteArrayInputStream(body);
        }

        @Override
        public int read() {
            return delegate.read();
        }

        @Override
        public boolean isFinished() {
            return delegate.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            throw new UnsupportedOperationException("Async IO is not supported");
        }
    }
}
