package io.sqm.render.ansi;

import io.sqm.core.DollarStringLiteralExpr;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.defaults.DefaultSqlWriter;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link DollarStringLiteralExprRenderer}.
 * <p>Dollar-quoted strings are not supported in ANSI SQL, so an {@link UnsupportedDialectFeatureException} should be thrown.</p>
 */
@DisplayName("ANSI DollarStringLiteralExprRenderer Tests")
class DollarStringLiteralExprRendererTest {

    private final RenderContext ctx = RenderContext.of(new AnsiDialect());
    private final DollarStringLiteralExprRenderer renderer = new DollarStringLiteralExprRenderer();

    @Test
    @DisplayName("Dollar-quoted string throws UnsupportedDialectFeatureException")
    void dollarStringThrowsException() {
        var dollarString = DollarStringLiteralExpr.of("", "hello");
        var writer = new DefaultSqlWriter(ctx);

        assertThrows(UnsupportedDialectFeatureException.class,
            () -> renderer.render(dollarString, ctx, writer),
            "Dollar-quoted strings should not be supported in ANSI SQL");
    }

    @Test
    @DisplayName("Dollar-quoted string with tag throws exception")
    void dollarStringWithTagThrowsException() {
        var dollarString = DollarStringLiteralExpr.of("tag", "content");
        var writer = new DefaultSqlWriter(ctx);

        assertThrows(UnsupportedDialectFeatureException.class,
            () -> renderer.render(dollarString, ctx, writer));
    }

    @Test
    @DisplayName("Dollar-quoted string with empty content throws exception")
    void dollarStringEmptyContentThrowsException() {
        var dollarString = DollarStringLiteralExpr.of("", "");
        var writer = new DefaultSqlWriter(ctx);

        assertThrows(UnsupportedDialectFeatureException.class,
            () -> renderer.render(dollarString, ctx, writer));
    }

    @Test
    @DisplayName("Dollar-quoted string with newlines throws exception")
    void dollarStringWithNewlinesThrowsException() {
        var dollarString = DollarStringLiteralExpr.of("", "hello\nworld");
        var writer = new DefaultSqlWriter(ctx);

        assertThrows(UnsupportedDialectFeatureException.class,
            () -> renderer.render(dollarString, ctx, writer));
    }

    @Test
    @DisplayName("UnsupportedDialectFeatureException has descriptive message")
    void exceptionMessageIsDescriptive() {
        var dollarString = DollarStringLiteralExpr.of("", "test");
        var writer = new DefaultSqlWriter(ctx);

        UnsupportedDialectFeatureException ex =
            assertThrows(UnsupportedDialectFeatureException.class,
                () -> renderer.render(dollarString, ctx, writer));
        String message = ex.getMessage();
        assertNotNull(message);
        assertTrue(message.toLowerCase().contains("dollar") || message.toLowerCase().contains("string"));
    }

    @Test
    @DisplayName("Target type is DollarStringLiteralExpr")
    void targetTypeIsDollarStringLiteralExpr() {
        assertEquals(DollarStringLiteralExpr.class, renderer.targetType());
    }
}
