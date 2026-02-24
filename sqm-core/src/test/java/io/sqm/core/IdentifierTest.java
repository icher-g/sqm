package io.sqm.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IdentifierTest {
    @Test
    void defaults_null_quote_style_to_none() {
        var id = new Identifier("users", null);
        assertEquals("users", id.value());
        assertEquals(QuoteStyle.NONE, id.quoteStyle());
        assertFalse(id.quoted());
    }

    @Test
    void quoted_flag_reflects_quote_style() {
        assertTrue(Identifier.of("User", QuoteStyle.DOUBLE_QUOTE).quoted());
        assertTrue(Identifier.of("User", QuoteStyle.BACKTICK).quoted());
        assertFalse(Identifier.of("user").quoted());
    }

    @Test
    void null_value_is_rejected() {
        assertThrows(NullPointerException.class, () -> new Identifier(null, QuoteStyle.NONE));
    }

    @Test
    void empty_value_is_rejected() {
        assertThrows(IllegalArgumentException.class, () -> Identifier.of(""));
    }

    @Test
    void blank_value_is_rejected() {
        assertThrows(IllegalArgumentException.class, () -> Identifier.of("   "));
    }
}
