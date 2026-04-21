package io.sqm.transpile;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TranspileWarningTest {

    @Test
    void storesWarningFields() {
        var warning = new TranspileWarning("APPROX", "Approximate rewrite applied");

        assertEquals("APPROX", warning.code());
        assertEquals("Approximate rewrite applied", warning.message());
        assertNull(warning.statementIndex());
    }

    @Test
    void withStatementIndexCopiesWarningContext() {
        var warning = new TranspileWarning("APPROX", "Approximate rewrite applied").withStatementIndex(2);

        assertEquals("APPROX", warning.code());
        assertEquals("Approximate rewrite applied", warning.message());
        assertEquals(2, warning.statementIndex());
    }

    @Test
    void withStatementIndexRejectsNonPositiveIndex() {
        var warning = new TranspileWarning("APPROX", "Approximate rewrite applied");

        assertThrows(IllegalArgumentException.class, () -> warning.withStatementIndex(0));
    }
}
