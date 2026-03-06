package io.sqm.render.mysql;

import io.sqm.core.Expression;
import io.sqm.core.LimitOffset;
import io.sqm.render.mysql.spi.MySqlDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MySqlLimitOffsetRendererTest {

    private final RenderContext context = RenderContext.of(new MySqlDialect());

    @Test
    void rendersLimitOnly() {
        var sql = normalize(context.render(LimitOffset.of(10L, null)).sql());
        assertEquals("LIMIT 10", sql);
    }

    @Test
    void rendersLimitWithOffset() {
        var sql = normalize(context.render(LimitOffset.of(10L, 5L)).sql());
        assertEquals("LIMIT 10 OFFSET 5", sql);
    }

    @Test
    void throwsOnOffsetWithoutLimit() {
        assertThrows(UnsupportedOperationException.class,
            () -> context.render(LimitOffset.of(null, 5L)).sql());
    }

    @Test
    void throwsOnLimitAll() {
        assertThrows(UnsupportedOperationException.class,
            () -> context.render(LimitOffset.of(null, Expression.literal(5L), true)).sql());
    }

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
