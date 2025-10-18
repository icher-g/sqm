package io.sqm.render.ansi;

import io.sqm.core.Column;
import io.sqm.core.SelectQuery;
import io.sqm.core.Table;
import io.sqm.render.DefaultSqlWriter;
import io.sqm.render.ansi.column.QueryColumnRenderer;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
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
        var context = RenderContext.of(new AnsiDialect());
        var writer = new DefaultSqlWriter(context);
        renderer.render(column, context, writer);
        assertEquals("(SELECT c1 FROM t) AS a", writer.toText(List.of()).sql());
    }
}