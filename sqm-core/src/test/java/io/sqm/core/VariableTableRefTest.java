package io.sqm.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VariableTableRefTest {

    @Test
    void createsVariableTableFromStringAndIdentifier() {
        var fromString = VariableTableRef.of("audit_rows");
        var fromIdentifier = VariableTableRef.of(Identifier.of("audit_rows"));

        assertEquals("audit_rows", fromString.name().value());
        assertEquals(fromString, fromIdentifier);
    }

    @Test
    void rejectsQuotedOrBlankVariableTableNames() {
        assertThrows(IllegalArgumentException.class,
            () -> VariableTableRef.of(Identifier.of("audit_rows", QuoteStyle.DOUBLE_QUOTE)));
        assertThrows(IllegalArgumentException.class, () -> VariableTableRef.of(" "));
    }
}
