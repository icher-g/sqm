package io.sqm.render.postgresql.spi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostgresPaginationStyleTest {

    private final PostgresPaginationStyle style = new PostgresPaginationStyle();

    @Test
    void supportsLimitOffsetOnly() {
        assertTrue(style.supportsLimitOffset());
        assertFalse(style.supportsOffsetFetch());
        assertFalse(style.supportsTop());
    }
}
