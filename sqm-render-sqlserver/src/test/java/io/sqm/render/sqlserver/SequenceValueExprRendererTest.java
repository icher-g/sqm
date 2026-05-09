package io.sqm.render.sqlserver;

import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.defaults.DefaultSqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.sqlserver.spi.SqlServerDialect;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.currentValue;
import static io.sqm.dsl.Dsl.nextValue;
import static io.sqm.dsl.Dsl.qualify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SequenceValueExprRendererTest {
    private final RenderContext ctx = RenderContext.of(new SqlServerDialect());

    @Test
    void rendersNextValueFor() {
        assertEquals("NEXT VALUE FOR app.users_seq", ctx.render(nextValue(qualify("app", "users_seq"))).sql());
    }

    @Test
    void rejectsCurrentValue() {
        assertThrows(UnsupportedDialectFeatureException.class, () -> ctx.render(currentValue("users_seq")));
    }

    @Test
    void rejectsWhenDialectDoesNotSupportSequenceValues() {
        var unsupportedCtx = RenderContext.of(new AnsiDialect());
        var writer = new DefaultSqlWriter(unsupportedCtx);

        assertThrows(
            UnsupportedDialectFeatureException.class,
            () -> new SequenceValueExprRenderer().render(nextValue("users_seq"), unsupportedCtx, writer)
        );
    }
}
