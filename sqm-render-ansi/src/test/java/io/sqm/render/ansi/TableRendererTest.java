package io.sqm.render.ansi;

import io.sqm.core.Table;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.defaults.DefaultSqlWriter;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TableRendererTest {

    private static String render(Table table) {
        RenderContext ctx = RenderContext.of(new AnsiDialect());
        SqlWriter w = new DefaultSqlWriter(ctx);
        w.append(table);
        return w.toText(List.of()).sql();
    }

    @Test
    @DisplayName("Renders simple table")
    void renders_table() {
        var sql = render(Table.of("t").as("a"));
        assertTrue(sql.contains("t"));
        assertTrue(sql.contains("AS a"));
    }

    @Test
    @DisplayName("Rejects ONLY in ANSI renderer")
    void rejects_only() {
        var table = Table.of("t").only();
        assertThrows(UnsupportedDialectFeatureException.class, () -> render(table));
    }

    @Test
    @DisplayName("Rejects table inheritance star in ANSI renderer")
    void rejects_inheritance_star() {
        var table = Table.of("t").includingDescendants();
        assertThrows(UnsupportedDialectFeatureException.class, () -> render(table));
    }
}
