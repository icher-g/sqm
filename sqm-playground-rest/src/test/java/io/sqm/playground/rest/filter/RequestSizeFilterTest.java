package io.sqm.playground.rest.filter;

import io.sqm.playground.rest.config.PlaygroundAbuseProtectionProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests request-size filtering behavior.
 */
class RequestSizeFilterTest {

    @Test
    void disabledSizeLimitPassesRequestThrough() throws Exception {
        var filter = new RequestSizeFilter(properties(0));
        var request = requestWithBody("/api/v1/parse", "{\"sql\":\"select 1\"}");
        var response = new MockHttpServletResponse();
        var invoked = new AtomicBoolean();

        filter.doFilter(request, response, flaggingChain(invoked));

        assertTrue(invoked.get());
        assertEquals(200, response.getStatus());
    }

    @Test
    void knownContentLengthWithinLimitPassesOriginalRequestThrough() throws Exception {
        var filter = new RequestSizeFilter(properties(64));
        var request = requestWithBody("/api/v1/parse", "{\"sql\":\"select 1\"}");
        var response = new MockHttpServletResponse();
        var invoked = new AtomicBoolean();

        filter.doFilter(request, response, flaggingChain(invoked));

        assertTrue(invoked.get());
        assertEquals("POST", request.getMethod());
    }

    @Test
    void unknownContentLengthCachesBodyBeforePassingDownstream() throws Exception {
        var filter = new RequestSizeFilter(properties(64));
        var request = unknownLengthRequest("/api/v1/parse", "{\"sql\":\"select 1\"}");
        var response = new MockHttpServletResponse();
        var seenBody = new AtomicReference<String>();

        filter.doFilter(request, response, capturingChain(seenBody));

        assertEquals("{\"sql\":\"select 1\"}", seenBody.get());
    }

    @Test
    void unknownContentLengthRejectsOversizedBody() throws Exception {
        var filter = new RequestSizeFilter(properties(4));
        var request = unknownLengthRequest("/api/v1/parse", "{\"sql\":\"select 1\"}");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            throw new AssertionError("filter should have rejected oversized body");
        });

        assertEquals(413, response.getStatus());
        assertTrue(response.getContentAsString().contains("REQUEST_TOO_LARGE"));
    }

    @Test
    void cachedBodyInputStreamRejectsAsyncReadListener() throws Exception {
        var filter = new RequestSizeFilter(properties(64));
        var request = unknownLengthRequest("/api/v1/parse", "{\"sql\":\"select 1\"}");
        var response = new MockHttpServletResponse();
        var unsupported = new AtomicBoolean();

        filter.doFilter(request, response, (req, res) -> {
            var cachedRequest = (HttpServletRequest) req;
            var stream = cachedRequest.getInputStream();
            assertFalse(stream.isFinished());
            assertTrue(stream.isReady());
            assertThrows(UnsupportedOperationException.class, () -> stream.setReadListener(new NoOpReadListener()));
            //noinspection StatementWithEmptyBody
            while (stream.read() != -1) {
                // consume fully so isFinished() can be verified
            }
            assertTrue(stream.isFinished());
            unsupported.set(true);
        });

        assertTrue(unsupported.get());
    }

    private static PlaygroundAbuseProtectionProperties properties(long maxBytes) {
        var properties = new PlaygroundAbuseProtectionProperties();
        properties.setMaxRequestBytes(maxBytes);
        return properties;
    }

    private static MockHttpServletRequest requestWithBody(String path, String body) {
        var request = new MockHttpServletRequest("POST", path);
        request.setContentType("application/json");
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        return request;
    }

    private static HttpServletRequest unknownLengthRequest(String path, String body) {
        return new HttpServletRequestWrapper(requestWithBody(path, body)) {
            @Override
            public long getContentLengthLong() {
                return -1;
            }

            @Override
            public ServletInputStream getInputStream() {
                var delegate = new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
                return new ServletInputStream() {
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
                };
            }
        };
    }

    private static FilterChain flaggingChain(AtomicBoolean invoked) {
        return (request, response) -> invoked.set(true);
    }

    private static FilterChain capturingChain(AtomicReference<String> seenBody) {
        return (request, response) -> seenBody.set(new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
    }

    private static final class NoOpReadListener implements ReadListener {
        @Override
        public void onDataAvailable() {
        }

        @Override
        public void onAllDataRead() {
        }

        @Override
        public void onError(Throwable throwable) {
        }
    }
}
