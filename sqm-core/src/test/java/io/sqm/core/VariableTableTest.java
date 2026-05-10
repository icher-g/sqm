package io.sqm.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VariableTableTest {

    @Test
    void createsVariableTableFromStringAndIdentifier() {
        var fromString = VariableTable.of("audit_rows");
        var fromIdentifier = VariableTable.of(Identifier.of("audit_rows"));

        assertEquals("audit_rows", fromString.name().value());
        assertEquals(fromString, fromIdentifier);
    }

    @Test
    void rejectsQuotedOrBlankVariableTableNames() {
        assertThrows(IllegalArgumentException.class,
            () -> VariableTable.of(Identifier.of("audit_rows", QuoteStyle.DOUBLE_QUOTE)));
        assertThrows(IllegalArgumentException.class, () -> VariableTable.of(" "));
    }
}
