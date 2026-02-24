package io.sqm.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QualifiedNameTest {

    @Test
    void preserves_parts_and_values() {
        var qn = new QualifiedName(List.of(
            Identifier.of("pg_catalog"),
            Identifier.of("Int4", QuoteStyle.DOUBLE_QUOTE)
        ));

        assertEquals(2, qn.parts().size());
        assertEquals(List.of("pg_catalog", "Int4"), qn.values());
        assertEquals(QuoteStyle.DOUBLE_QUOTE, qn.parts().get(1).quoteStyle());
    }

    @Test
    void rejects_empty_parts() {
        assertThrows(IllegalArgumentException.class, () -> new QualifiedName(List.of()));
    }
}
