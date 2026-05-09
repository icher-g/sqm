package io.sqm.render.ansi;

import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.nextValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SequenceValueExprRendererTest {
    @Test
    void rejectsSequenceValueExpressions() {
        assertThrows(
            UnsupportedDialectFeatureException.class,
            () -> RenderContext.of(new AnsiDialect()).render(nextValue("users_seq"))
        );
    }
}
