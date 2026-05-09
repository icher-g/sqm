package io.sqm.render.postgresql;

import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.defaults.DefaultSqlWriter;
import io.sqm.render.postgresql.spi.PostgresDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.currentValue;
import static io.sqm.dsl.Dsl.nextValue;
import static io.sqm.dsl.Dsl.qualify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SequenceValueExprRendererTest {
    private final RenderContext ctx = RenderContext.of(new PostgresDialect());

    @Test
    void rendersPostgresSequenceValues() {
        assertEquals("nextval('users_seq')", ctx.render(nextValue("users_seq")).sql());
        assertEquals("currval('app.users_seq')", ctx.render(currentValue(qualify("app", "users_seq"))).sql());
    }

    @Test
    void escapesSequenceLiteralNames() {
        assertEquals("nextval('user''s_seq')", ctx.render(nextValue("user's_seq")).sql());
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
