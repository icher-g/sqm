package io.sqm.render.ansi;

import io.sqm.core.ArrayExpr;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for ArrayExprRenderer.
 * Array expressions are not supported in ANSI SQL, so an UnsupportedDialectFeatureException should be thrown.
 */
class ArrayExprRendererTest {

    private final RenderContext ctx = RenderContext.of(new AnsiDialect());
    private final ArrayExprRenderer renderer = new ArrayExprRenderer();

    @Test
    @DisplayName("Array expression throws UnsupportedDialectFeatureException")
    void array_expr_throws_exception() {
        // Create a mock ArrayExpr - since ArrayExpr is likely an interface/abstract class,
        // we test the renderer directly
        assertThrows(UnsupportedDialectFeatureException.class, () -> renderer.render(null, ctx, null),
            "Array expressions should not be supported in ANSI SQL");
    }

    @Test
    @DisplayName("UnsupportedDialectFeatureException has descriptive message")
    void exception_message_is_descriptive() {
        UnsupportedDialectFeatureException ex =
            assertThrows(UnsupportedDialectFeatureException.class, () -> renderer.render(null, ctx, null));
        String message = ex.getMessage();
        assertTrue(message != null && message.toLowerCase().contains("array")
        );
    }

    @Test
    @DisplayName("Target type is ArrayExpr")
    void target_type_is_array_expr() {
        Class<?> targetType = renderer.targetType();
        assertEquals(targetType);
    }

    private void assertTrue(boolean condition) {
        if (!condition) {
            throw new AssertionError("Exception message should mention Array");
        }
    }

    private void assertEquals(Object actual) {
        if (!ArrayExpr.class.equals(actual)) {
            throw new AssertionError("Expected: " + ArrayExpr.class + ", but got: " + actual);
        }
    }
}
