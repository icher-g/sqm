package io.sqm.render.ansi;

import io.sqm.core.Column;
import io.sqm.render.DefaultSqlWriter;
import io.sqm.render.ansi.column.NamedColumnRenderer;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NamedColumnRendererTest {

    @Test
    @DisplayName("MyTable.MyColumn AS Alias")
    void table_column_alias() {
        var column = Column.of("MyColumn").from("MyTable").as("Alias");
        var renderer = new NamedColumnRenderer();
        var context = RenderContext.of(new AnsiDialect());
        var writer = new DefaultSqlWriter(context);
        renderer.render(column, context, writer);
        assertEquals("MyTable.MyColumn AS Alias", writer.toText(List.of()).sql());
    }

    @Test
    @DisplayName("\"Table\".MyColumn AS Alias")
    void tableAsKeyword_column_alias() {
        var column = Column.of("MyColumn").from("Table").as("Alias");
        var renderer = new NamedColumnRenderer();
        var context = RenderContext.of(new AnsiDialect());
        var writer = new DefaultSqlWriter(context);
        renderer.render(column, context, writer);
        assertEquals("\"Table\".MyColumn AS Alias", writer.toText(List.of()).sql());
    }

    @Test
    @DisplayName("MyTable.\"Order\" AS Alias")
    void table_columnAsKeyword_alias() {
        var column = Column.of("Order").from("MyTable").as("Alias");
        var renderer = new NamedColumnRenderer();
        var context = RenderContext.of(new AnsiDialect());
        var writer = new DefaultSqlWriter(context);
        renderer.render(column, context, writer);
        assertEquals("MyTable.\"Order\" AS Alias", writer.toText(List.of()).sql());
    }

    @Test
    @DisplayName("MyTable.MyColumn AS \"Order\"")
    void table_column_aliasAsKeyword() {
        var column = Column.of("MyColumn").from("MyTable").as("Order");
        var renderer = new NamedColumnRenderer();
        var context = RenderContext.of(new AnsiDialect());
        var writer = new DefaultSqlWriter(context);
        renderer.render(column, context, writer);
        assertEquals("MyTable.MyColumn AS \"Order\"", writer.toText(List.of()).sql());
    }

    @Test
    @DisplayName("MyColumn AS Alias")
    void column_alias() {
        var column = Column.of("MyColumn").as("Alias");
        var renderer = new NamedColumnRenderer();
        var context = RenderContext.of(new AnsiDialect());
        var writer = new DefaultSqlWriter(context);
        renderer.render(column, context, writer);
        assertEquals("MyColumn AS Alias", writer.toText(List.of()).sql());
    }
}