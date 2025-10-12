package io.cherlabs.sqm.render.ansi;

import io.cherlabs.sqm.core.Column;
import io.cherlabs.sqm.core.SelectQuery;
import io.cherlabs.sqm.core.Table;
import io.cherlabs.sqm.render.DefaultSqlWriter;
import io.cherlabs.sqm.render.ansi.spi.AnsiRenderContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QueryColumnRendererTest {

    @Test
    @DisplayName("(SELECT c1 FROM t) AS a")
    void render_query_as_column() {
        var query = new SelectQuery();
        query.columns().add(Column.of("c1"));
        query.from(Table.of("t"));
        var column = Column.of(query).as("a");
        var renderer = new QueryColumnRenderer();
        var context = new AnsiRenderContext();
        var writer = new DefaultSqlWriter(context);
        renderer.render(column, context, writer);
        assertEquals("(SELECT c1 FROM t) AS a", writer.toText(List.of()).sql());
    }
}