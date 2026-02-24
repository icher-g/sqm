package io.sqm.core.walk;

import io.sqm.core.ColumnExpr;
import io.sqm.core.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Verifies that RecursiveNodeVisitor's {@code defaultResult()} is returned
 * when no overrides are made, and that a custom marker value is respected.
 *
 * <p>Adjust package names/imports to your project.</p>
 */
public class DefaultResultPropagationTest {

    @Test
    void defaultResult_isNullByDefault() {
        // Build any simple node; here we assume a factory exists
        ColumnExpr col = ColumnExpr.of(Identifier.of("u"), Identifier.of("id"));

        String result = col.accept(new NoopVisitor());
        assertNull(result, "Expected null when defaultResult() returns null");
    }

    @Test
    void defaultResult_customMarkerIsPropagated() {
        ColumnExpr col = ColumnExpr.of(Identifier.of("u"), Identifier.of("id"));

        String result = col.accept(new MarkerVisitor());
        assertEquals("__DEFAULT__", result, "Expected custom default marker to be returned");
    }

    /**
     * A trivial concrete visitor that doesn't override specific {@code visit*} methods
     * and leaves recursion behavior to the superclass.
     */
    static class NoopVisitor extends RecursiveNodeVisitor<String> {
        @Override
        protected String defaultResult() {
            return null; // default is null in many implementations
        }
    }

    static class MarkerVisitor extends RecursiveNodeVisitor<String> {
        @Override
        protected String defaultResult() {
            return "__DEFAULT__";
        }
    }
}
