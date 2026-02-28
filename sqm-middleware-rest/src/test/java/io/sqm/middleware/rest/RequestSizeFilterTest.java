package io.sqm.middleware.rest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class RequestSizeFilterTest {

    @Test
    void passes_through_when_max_bytes_is_disabled() throws Exception {
        var properties = new RestAbuseProtectionProperties();
        properties.setMaxRequestBytes(0);
        var filter = new RequestSizeFilter(properties);

        var request = new MockHttpServletRequest();
        request.setRequestURI("/sqm/middleware/analyze");
        request.setContent("payload".getBytes(StandardCharsets.UTF_8));
        var response = new MockHttpServletResponse();
        var called = new AtomicBoolean(false);

        filter.doFilter(request, response, new CapturingChain(called));
        assertTrue(called.get());
        assertEquals(200, response.getStatus());
    }

    @Test
    void rejects_request_when_declared_content_length_exceeds_max() throws Exception {
        var properties = new RestAbuseProtectionProperties();
        properties.setMaxRequestBytes(1);
        var filter = new RequestSizeFilter(properties);

        var request = new MockHttpServletRequest();
        request.setRequestURI("/sqm/middleware/analyze");
        request.setContent("payload".getBytes(StandardCharsets.UTF_8));
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> fail("filter chain must not be invoked"));
        assertEquals(413, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"REQUEST_TOO_LARGE\""));
    }

    @Test
    void reads_and_wraps_request_body_when_content_length_is_unknown() throws Exception {
        var properties = new RestAbuseProtectionProperties();
        properties.setMaxRequestBytes(10);
        var filter = new RequestSizeFilter(properties);

        var original = new MockHttpServletRequest();
        original.setRequestURI("/sqm/middleware/analyze");
        original.setContent("abc".getBytes(StandardCharsets.UTF_8));
        var request = new HttpServletRequestWrapper(original) {
            @Override
            public long getContentLengthLong() {
                return -1;
            }
        };

        var response = new MockHttpServletResponse();
        var called = new AtomicBoolean(false);

        filter.doFilter(request, response, (req, res) -> {
            called.set(true);
            var input = req.getInputStream();
            assertTrue(input.isReady());
            assertFalse(input.isFinished());
            assertThrows(UnsupportedOperationException.class, () -> input.setReadListener(null));
            assertEquals('a', input.read());
        });

        assertTrue(called.get());
        assertEquals(200, response.getStatus());
    }

    private record CapturingChain(AtomicBoolean called) implements FilterChain {
        @Override
        public void doFilter(ServletRequest request, ServletResponse response) {
            called.set(true);
        }
    }
}
