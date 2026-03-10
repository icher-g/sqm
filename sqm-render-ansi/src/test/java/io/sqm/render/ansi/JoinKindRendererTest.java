package io.sqm.render.ansi;

import io.sqm.core.JoinKind;
import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.core.dialect.VersionedDialectCapabilities;
import io.sqm.render.defaults.DefaultSqlWriter;
import io.sqm.render.SqlWriter;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for JoinKindRenderer.
 */
class JoinKindRendererTest {

    private final JoinKindRenderer renderer = new JoinKindRenderer();
    private final RenderContext ctx = RenderContext.of(new AnsiDialect());

    @Test
    @DisplayName("INNER join kind renders as 'INNER JOIN'")
    void inner_join() {
        SqlWriter w = new DefaultSqlWriter(ctx);
        renderer.render(JoinKind.INNER, ctx, w);
        assertEquals("INNER JOIN", w.toText(null).sql());
    }

    @Test
    @DisplayName("LEFT join kind renders as 'LEFT JOIN'")
    void left_join() {
        SqlWriter w = new DefaultSqlWriter(ctx);
        renderer.render(JoinKind.LEFT, ctx, w);
        assertEquals("LEFT JOIN", w.toText(null).sql());
    }

    @Test
    @DisplayName("RIGHT join kind renders as 'RIGHT JOIN'")
    void right_join() {
        SqlWriter w = new DefaultSqlWriter(ctx);
        renderer.render(JoinKind.RIGHT, ctx, w);
        assertEquals("RIGHT JOIN", w.toText(null).sql());
    }

    @Test
    @DisplayName("FULL join kind renders as 'FULL JOIN'")
    void full_join() {
        SqlWriter w = new DefaultSqlWriter(ctx);
        renderer.render(JoinKind.FULL, ctx, w);
        assertEquals("FULL JOIN", w.toText(null).sql());
    }

    @Test
    @DisplayName("ANSI-supported join kinds render with JOIN keyword")
    void all_join_kinds_contain_join() {
        for (JoinKind kind : new JoinKind[]{JoinKind.INNER, JoinKind.LEFT, JoinKind.RIGHT, JoinKind.FULL}) {
            SqlWriter w = new DefaultSqlWriter(ctx);
            renderer.render(kind, ctx, w);
            String result = w.toText(null).sql();
            assertTrue(result.contains("JOIN"), kind + " should render with JOIN keyword");
        }
    }

    @Test
    @DisplayName("STRAIGHT_JOIN is rejected in ANSI dialect")
    void straight_join_is_rejected_for_ansi() {
        SqlWriter w = new DefaultSqlWriter(ctx);
        assertThrows(UnsupportedDialectFeatureException.class, () -> renderer.render(JoinKind.STRAIGHT, ctx, w));
    }

    @Test
    @DisplayName("STRAIGHT join kind renders when the dialect supports it")
    void straight_join_renders_when_supported() {
        var straightCtx = RenderContext.of(new StraightAnsiDialect());
        SqlWriter w = new DefaultSqlWriter(straightCtx);

        renderer.render(JoinKind.STRAIGHT, straightCtx, w);

        assertEquals("STRAIGHT_JOIN", w.toText(null).sql());
    }

    private static final class StraightAnsiDialect extends AnsiDialect {
        @Override
        public DialectCapabilities capabilities() {
            return VersionedDialectCapabilities.builder(SqlDialectVersion.of(2016))
                .supports(SqlDialectVersion.minimum(), SqlFeature.values())
                .build();
        }
    }
}
