package io.sqm.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TopSpecTest {

    @Test
    void creates_plain_top_spec() {
        var top = TopSpec.of(Expression.literal(10));

        assertEquals(Expression.literal(10), top.count());
        assertFalse(top.percent());
        assertFalse(top.withTies());
    }

    @Test
    void creates_percent_and_with_ties_spec() {
        var top = TopSpec.of(Expression.literal(25), true, true);

        assertTrue(top.percent());
        assertTrue(top.withTies());
    }

    @Test
    void rejects_null_count() {
        assertThrows(NullPointerException.class, () -> TopSpec.of(null, false, false));
    }
}
