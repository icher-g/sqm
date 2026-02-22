package io.sqm.render.ansi;

import io.sqm.core.Lateral;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.defaults.DefaultSqlWriter;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link LateralRenderer}.
 * <p>LATERAL is not supported in ANSI SQL, so an {@link UnsupportedDialectFeatureException} should be thrown.</p>
 */
@DisplayName("ANSI LateralRenderer Tests")
class LateralRendererTest {

    private final RenderContext ctx = RenderContext.of(new AnsiDialect());
    private final LateralRenderer renderer = new LateralRenderer();

    @Test
    @DisplayName("LATERAL with query table throws UnsupportedDialectFeatureException")
    void lateralWithQueryTableThrowsException() {
        var query = select(col("id")).from(tbl("users")).build();
        var lateral = Lateral.of(tbl(query).as("t"));
        var writer = new DefaultSqlWriter(ctx);

        assertThrows(UnsupportedDialectFeatureException.class,
            () -> renderer.render(lateral, ctx, writer),
            "LATERAL should not be supported in ANSI SQL");
    }

    @Test
    @DisplayName("LATERAL with function table throws UnsupportedDialectFeatureException")
    void lateralWithFunctionTableThrowsException() {
        var funcTable = tbl(func("unnest", arg(col("arr"))));
        var lateral = Lateral.of(funcTable.as("t"));
        var writer = new DefaultSqlWriter(ctx);

        assertThrows(UnsupportedDialectFeatureException.class,
            () -> renderer.render(lateral, ctx, writer));
    }

    @Test
    @DisplayName("UnsupportedDialectFeatureException has descriptive message")
    void exceptionMessageIsDescriptive() {
        var query = select(col("id")).from(tbl("users")).build();
        var lateral = Lateral.of(tbl(query).as("t"));
        var writer = new DefaultSqlWriter(ctx);

        UnsupportedDialectFeatureException ex =
            assertThrows(UnsupportedDialectFeatureException.class,
                () -> renderer.render(lateral, ctx, writer));
        String message = ex.getMessage();
        assertNotNull(message);
        assertTrue(message.toUpperCase().contains("LATERAL"));
    }

    @Test
    @DisplayName("Target type is Lateral")
    void targetTypeIsLateral() {
        assertEquals(Lateral.class, renderer.targetType());
    }
}
