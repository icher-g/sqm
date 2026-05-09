package io.sqm.render.oracle;

import io.sqm.render.oracle.spi.OracleDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.currentValue;
import static io.sqm.dsl.Dsl.nextValue;
import static io.sqm.dsl.Dsl.qualify;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SequenceValueExprRendererTest {
    private final RenderContext ctx = RenderContext.of(new OracleDialect());

    @Test
    void rendersOracleSequenceValues() {
        assertEquals("users_seq.NEXTVAL", ctx.render(nextValue("users_seq")).sql());
        assertEquals("app.users_seq.CURRVAL", ctx.render(currentValue(qualify("app", "users_seq"))).sql());
    }
}
