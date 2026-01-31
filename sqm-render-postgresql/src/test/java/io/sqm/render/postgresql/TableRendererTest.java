package io.sqm.render.postgresql;

import io.sqm.core.Table;
import io.sqm.render.SqlWriter;
import io.sqm.render.defaults.DefaultSqlWriter;
import io.sqm.render.postgresql.spi.PostgresDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TableRendererTest {

    private static String render(Table table) {
        RenderContext ctx = RenderContext.of(new PostgresDialect());
        SqlWriter w = new DefaultSqlWriter(ctx);
        w.append(table);
        return w.toText(List.of()).sql();
    }

    @Test
    @DisplayName("Renders ONLY table")
    void renders_only_table() {
        var sql = render(Table.of("t").only());
        assertTrue(sql.startsWith("ONLY "));
        assertTrue(sql.contains("t"));
    }

    @Test
    @DisplayName("Renders table inheritance star")
    void renders_table_inheritance_star() {
        var sql = render(Table.of("t").includingDescendants().as("a"));
        assertTrue(sql.contains("t *"));
        assertTrue(sql.contains("AS a"));
    }
}
