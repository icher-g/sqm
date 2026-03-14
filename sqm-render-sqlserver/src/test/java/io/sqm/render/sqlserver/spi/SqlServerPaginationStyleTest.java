package io.sqm.render.sqlserver.spi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlServerPaginationStyleTest {

    private final SqlServerPaginationStyle style = new SqlServerPaginationStyle();

    @Test
    void supports_top_and_offset_fetch() {
        assertFalse(style.supportsLimitOffset());
        assertTrue(style.supportsOffsetFetch());
        assertTrue(style.supportsTop());
    }
}
