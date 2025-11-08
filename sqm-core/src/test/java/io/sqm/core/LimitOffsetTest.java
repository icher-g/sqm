package io.sqm.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LimitOffsetTest {

    @Test
    void limit() {
        var lo = LimitOffset.limit(1L);
        assertEquals(1, lo.limit());
    }

    @Test
    void offset() {
        var lo = LimitOffset.offset(1L);
        assertEquals(1, lo.offset());
    }
}