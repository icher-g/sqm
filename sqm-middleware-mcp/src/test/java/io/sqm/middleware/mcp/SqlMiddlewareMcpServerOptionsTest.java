package io.sqm.middleware.mcp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlMiddlewareMcpServerOptionsTest {

    @Test
    void validates_positive_limits() {
        assertThrows(IllegalArgumentException.class, () -> new SqlMiddlewareMcpServerOptions(0, 1, 1, true));
        assertThrows(IllegalArgumentException.class, () -> new SqlMiddlewareMcpServerOptions(1, 0, 1, true));
        assertThrows(IllegalArgumentException.class, () -> new SqlMiddlewareMcpServerOptions(1, 1, 0, true));
    }

    @Test
    void exposes_default_hardening_values() {
        var defaults = SqlMiddlewareMcpServerOptions.defaults();
        assertTrue(defaults.maxContentLengthBytes() > 0);
        assertTrue(defaults.maxHeaderLineLengthBytes() > 0);
        assertTrue(defaults.maxHeaderBytes() > 0);
    }
}
