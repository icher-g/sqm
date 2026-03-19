package io.sqm.render.sqlserver;

import io.sqm.core.Expression;
import io.sqm.core.LimitOffset;
import io.sqm.render.defaults.DefaultSqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.sqlserver.spi.SqlServerDialect;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LimitOffsetRendererTest {

    private final LimitOffsetRenderer renderer = new LimitOffsetRenderer();
    private final RenderContext context = RenderContext.of(new SqlServerDialect());

    @Test
    void renders_offset_fetch() {
        var writer = new DefaultSqlWriter(context);

        renderer.render(LimitOffset.of(Expression.literal(10), Expression.literal(5)), context, writer);

        assertEquals("OFFSET 5 ROWS FETCH NEXT 10 ROWS ONLY", normalize(writer.toText(List.of()).sql()));
    }

    @Test
    void skips_limit_only_for_top_rendering() {
        var writer = new DefaultSqlWriter(context);

        renderer.render(LimitOffset.limit(10), context, writer);

        assertEquals("", normalize(writer.toText(List.of()).sql()));
    }

    @Test
    void rejects_limit_all() {
        assertThrows(UnsupportedOperationException.class, () ->
            renderer.render(LimitOffset.all(), context, new DefaultSqlWriter(context)));
    }

    @Test
    void renders_offset_only() {
        var writer = new DefaultSqlWriter(context);

        renderer.render(LimitOffset.offset(5), context, writer);

        assertEquals("OFFSET 5 ROWS", normalize(writer.toText(List.of()).sql()));
    }

    @Test
    void skips_empty_limit_offset() {
        var writer = new DefaultSqlWriter(context);

        renderer.render(LimitOffset.of(null, null, false), context, writer);

        assertEquals("", normalize(writer.toText(List.of()).sql()));
    }

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
