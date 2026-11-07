package io.sqm.render.sqlserver;

import io.sqm.render.spi.RenderContext;
import io.sqm.render.sqlserver.spi.SqlServerDialect;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class CompositeQueryRendererTest {
    private final RenderContext ctx = RenderContext.of(new SqlServerDialect());

    @Test
    void rendersSetPaginationWithoutDroppingTheLimit() {
        var query = select(lit(1L)).build().union(select(lit(2L)).build()).orderBy(1).limit(5L);
        var sql = ctx.render(query).sql().replaceAll("\\s+", " ").trim();
        assertTrue(sql.endsWith("ORDER BY 1 OFFSET 0 ROWS FETCH NEXT 5 ROWS ONLY"), sql);
        assertNull(query.limitOffset().offset());
        var offset = ctx.render(query.offset(2L)).sql().replaceAll("\\s+", " ").trim();
        assertTrue(offset.endsWith("ORDER BY 1 OFFSET 2 ROWS FETCH NEXT 5 ROWS ONLY"), offset);
    }

    @Test
    void rejectsPaginationWithoutOrderingAndLimitAll() {
        var query = select(lit(1L)).build().union(select(lit(2L)).build());
        assertThrows(UnsupportedOperationException.class, () -> ctx.render(query.offset(2L)));
        assertThrows(UnsupportedOperationException.class, () -> ctx.render(query.limit(5L)));
        var all = compose(query.terms(), query.ops(), orderBy(order(1)), limitAll());
        assertThrows(UnsupportedOperationException.class, () -> ctx.render(all));
    }

    @Test
    void rendersUnpaginatedResults() {
        var query = select(lit(1L)).build().union(select(lit(2L)).build());
        assertFalse(ctx.render(query).sql().contains("OFFSET"));
        assertFalse(ctx.render(compose(query.terms(), query.ops(), null, limitOffset(null, null)))
            .sql().contains("OFFSET"));
    }
}
