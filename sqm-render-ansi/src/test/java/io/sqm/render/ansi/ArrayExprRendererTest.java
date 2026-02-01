package io.sqm.render.ansi;

import io.sqm.core.ArrayExpr;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.defaults.DefaultSqlWriter;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.array;
import static io.sqm.dsl.Dsl.lit;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ArrayExprRenderer}.
 * <p>Array expressions are not supported in ANSI SQL, so an {@link UnsupportedDialectFeatureException} should be thrown.</p>
 */
@DisplayName("ANSI ArrayExprRenderer Tests")
class ArrayExprRendererTest {

    private final RenderContext ctx = RenderContext.of(new AnsiDialect());
    private final ArrayExprRenderer renderer = new ArrayExprRenderer();

    @Test
    @DisplayName("Array expression throws UnsupportedDialectFeatureException")
    void arrayExprThrowsException() {
        var arrayExpr = array(lit(1), lit(2), lit(3));
        var writer = new DefaultSqlWriter(ctx);

        assertThrows(UnsupportedDialectFeatureException.class,
            () -> renderer.render(arrayExpr, ctx, writer),
            "Array expressions should not be supported in ANSI SQL");
    }

    @Test
    @DisplayName("Empty array expression throws exception")
    void emptyArrayThrowsException() {
        var arrayExpr = array();
        var writer = new DefaultSqlWriter(ctx);

        assertThrows(UnsupportedDialectFeatureException.class,
            () -> renderer.render(arrayExpr, ctx, writer));
    }

    @Test
    @DisplayName("Nested array expression throws exception")
    void nestedArrayThrowsException() {
        var arrayExpr = array(array(lit(1)));
        var writer = new DefaultSqlWriter(ctx);

        assertThrows(UnsupportedDialectFeatureException.class,
            () -> renderer.render(arrayExpr, ctx, writer));
    }

    @Test
    @DisplayName("UnsupportedDialectFeatureException has descriptive message")
    void exceptionMessageIsDescriptive() {
        var arrayExpr = array(lit(1));
        var writer = new DefaultSqlWriter(ctx);

        UnsupportedDialectFeatureException ex =
            assertThrows(UnsupportedDialectFeatureException.class,
                () -> renderer.render(arrayExpr, ctx, writer));
        String message = ex.getMessage();
        assertNotNull(message);
        assertTrue(message.toLowerCase().contains("array"));
    }

    @Test
    @DisplayName("Target type is ArrayExpr")
    void targetTypeIsArrayExpr() {
        assertEquals(ArrayExpr.class, renderer.targetType());
    }
}
