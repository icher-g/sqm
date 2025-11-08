package io.sqm.core.internal;

import io.sqm.core.LimitOffset;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LimitOffsetImplTest {

    @Test
    void negative() {
        assertThrows(IllegalArgumentException.class, () -> LimitOffset.limit(-1L));
        assertThrows(IllegalArgumentException.class, () -> LimitOffset.offset(-1L));
    }
}