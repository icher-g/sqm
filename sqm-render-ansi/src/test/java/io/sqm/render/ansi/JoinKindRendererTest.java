package io.sqm.render.ansi;

import io.sqm.core.JoinKind;
import io.sqm.render.DefaultSqlWriter;
import io.sqm.render.SqlWriter;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    @DisplayName("All join kinds render with JOIN keyword")
    void all_join_kinds_contain_join() {
        for (JoinKind kind : JoinKind.values()) {
            SqlWriter w = new DefaultSqlWriter(ctx);
            renderer.render(kind, ctx, w);
            String result = w.toText(null).sql();
            assertTrue(result.contains("JOIN"), kind + " should render with JOIN keyword");
        }
    }
}
