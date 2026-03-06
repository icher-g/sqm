package io.sqm.render.mysql.spi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MySqlPaginationStyleTest {

    private final MySqlPaginationStyle style = new MySqlPaginationStyle();

    @Test
    void supports_limit_offset_only() {
        assertTrue(style.supportsLimitOffset());
        assertFalse(style.supportsOffsetFetch());
        assertFalse(style.supportsTop());
    }
}
