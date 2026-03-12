package io.sqm.transpile;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TranspileWarningTest {

    @Test
    void storesWarningFields() {
        var warning = new TranspileWarning("APPROX", "Approximate rewrite applied");

        assertEquals("APPROX", warning.code());
        assertEquals("Approximate rewrite applied", warning.message());
    }
}
