package io.sqm.render.ansi;

import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.render.defaults.DefaultSqlWriter;
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

    @Test
    void rejectsEvenWhenFeatureIsEnabledForAnsiRenderer() {
        var ctx = RenderContext.of(new AnsiDialect() {
            @Override
            public io.sqm.core.dialect.DialectCapabilities capabilities() {
                return feature -> feature == SqlFeature.SEQUENCE_VALUE_EXPRESSION;
            }
        });
        var writer = new DefaultSqlWriter(ctx);

        assertThrows(
            UnsupportedDialectFeatureException.class,
            () -> new SequenceValueExprRenderer().render(nextValue("users_seq"), ctx, writer)
        );
    }
}
