package io.sqm.render.ansi;

import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ColumnExprRendererTest {

    @Test
    @DisplayName("MyTable.MyColumn AS Alias")
    void table_column_alias() {
        var column = col("MyColumn").inTable("MyTable").as("Alias");
        var ctx = RenderContext.of(new AnsiDialect());
        var text = ctx.render(column);
        assertEquals("MyTable.MyColumn AS Alias", text.sql());
    }

    @Test
    @DisplayName("\"Table\".MyColumn AS Alias")
    void tableAsKeyword_column_alias() {
        var column = col("MyColumn").inTable("Table").as("Alias");
        var ctx = RenderContext.of(new AnsiDialect());
        var text = ctx.render(column);
        assertEquals("\"Table\".MyColumn AS Alias", text.sql());
    }

    @Test
    @DisplayName("MyTable.\"Order\" AS Alias")
    void table_columnAsKeyword_alias() {
        var column = col("Order").inTable("MyTable").as("Alias");
        var ctx = RenderContext.of(new AnsiDialect());
        var text = ctx.render(column);
        assertEquals("MyTable.\"Order\" AS Alias", text.sql());
    }

    @Test
    @DisplayName("MyTable.MyColumn AS \"Order\"")
    void table_column_aliasAsKeyword() {
        var column = col("MyColumn").inTable("MyTable").as("Order");
        var ctx = RenderContext.of(new AnsiDialect());
        var text = ctx.render(column);
        assertEquals("MyTable.MyColumn AS \"Order\"", text.sql());
    }

    @Test
    @DisplayName("MyColumn AS Alias")
    void column_alias() {
        var column = col("MyColumn").as("Alias");
        var ctx = RenderContext.of(new AnsiDialect());
        var text = ctx.render(column);
        assertEquals("MyColumn AS Alias", text.sql());
    }
}