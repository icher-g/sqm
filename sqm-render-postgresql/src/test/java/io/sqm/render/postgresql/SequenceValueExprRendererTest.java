package io.sqm.render.postgresql;

import io.sqm.render.postgresql.spi.PostgresDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.currentValue;
import static io.sqm.dsl.Dsl.nextValue;
import static io.sqm.dsl.Dsl.qualify;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SequenceValueExprRendererTest {
    private final RenderContext ctx = RenderContext.of(new PostgresDialect());

    @Test
    void rendersPostgresSequenceValues() {
        assertEquals("nextval('users_seq')", ctx.render(nextValue("users_seq")).sql());
        assertEquals("currval('app.users_seq')", ctx.render(currentValue(qualify("app", "users_seq"))).sql());
    }
}
