package io.cherlabs.sqlmodel.render.ansi;

import io.cherlabs.sqlmodel.core.FunctionColumn;
import io.cherlabs.sqlmodel.render.DefaultSqlWriter;
import io.cherlabs.sqlmodel.render.SqlWriter;
import io.cherlabs.sqlmodel.render.ansi.spi.AnsiRenderContext;
import io.cherlabs.sqlmodel.render.spi.RenderContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.cherlabs.sqlmodel.core.FunctionColumn.Arg.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FunctionColumnRendererTest {

    private final FunctionColumnRenderer renderer = new FunctionColumnRenderer();

    /**
     * Helper to render a function column and return the SQL string.
     */
    private String render(FunctionColumn f) {
        RenderContext ctx = new AnsiRenderContext();
        SqlWriter w = new DefaultSqlWriter(ctx);
        renderer.render(f, ctx, w);
        return w.toText(List.of()).sql(); // If your SqlText accessor differs, adjust here.
    }

    @Test
    @DisplayName("Bare function with column arg and alias")
    void bareFunc_withColumnArg_andAlias() {
        var fc = FunctionColumn.of("lower", column("t", "name")).as("lname");
        var sql = render(fc);
        assertEquals("lower(t.name) AS lname", sql);
    }

    @Test
    @DisplayName("Schema-qualified function name")
    void qualifiedFuncName() {
        var fc = FunctionColumn.of("pg_catalog.lower", column("name"));
        var sql = render(fc);
        assertEquals("pg_catalog.lower(name)", sql);
    }

    @Test
    @DisplayName("DISTINCT inside call with single column")
    void distinct_singleColumn() {
        var fc = new FunctionColumn(
                "count",
                List.of(column("t", "id")),
                true,   // DISTINCT
                null
        );
        var sql = render(fc);
        assertEquals("count(DISTINCT t.id)", sql);
    }

    @Test
    @DisplayName("COUNT(*) without alias")
    void count_star_noAlias() {
        var fc = FunctionColumn.of("count", star());
        var sql = render(fc);
        assertEquals("count(*)", sql);
    }

    @Test
    @DisplayName("ColumnRef without table")
    void columnRef_withoutTable() {
        var fc = FunctionColumn.of("upper", column("city"));
        var sql = render(fc);
        assertEquals("upper(city)", sql);
    }

    @Test
    @DisplayName("ColumnRef with table")
    void columnRef_withTable() {
        var fc = FunctionColumn.of("upper", column("addr", "city"));
        var sql = render(fc);
        assertEquals("upper(addr.city)", sql);
    }

    @Test
    @DisplayName("Literals: NULL, booleans, numbers, strings (escaping)")
    void literals_various() {
        var fc = new FunctionColumn(
                "coalesce",
                List.of(
                        lit(null),        // NULL
                        lit(true),        // TRUE
                        lit(42),          // number
                        lit(3.14),        // decimal
                        lit("O'Reilly")   // string with quote to be doubled
                ),
                false,
                null
        );
        var sql = render(fc);
        assertEquals("coalesce(NULL, TRUE, 42, 3.14, 'O''Reilly')", sql);
    }

    @Test
    @DisplayName("String concatenation example with multiple literals")
    void concat_withLiterals() {
        var fc = new FunctionColumn(
                "concat",
                List.of(
                        lit("Hello"),
                        lit(", "),
                        lit("World")
                ),
                false,
                "greeting"
        );
        var sql = render(fc);
        assertEquals("concat('Hello', ', ', 'World') AS greeting", sql);
    }

    @Test
    @DisplayName("Function with no arguments renders empty parens")
    void noArguments() {
        var fc = FunctionColumn.of("random");
        var sql = render(fc);
        assertEquals("random()", sql);
    }

    @Test
    @DisplayName("DISTINCT with multiple args (generic function)")
    void distinct_multipleArgs() {
        var fc = new FunctionColumn(
                "some_func",
                List.of(column("t", "a"), column("t", "b")),
                true,
                null
        );
        var sql = render(fc);
        // DISTINCT prefix is applied once before the full comma-separated list
        assertEquals("some_func(DISTINCT t.a, t.b)", sql);
    }

    @Test
    @DisplayName("Nested function as argument")
    void nestedFuncArg() {
        var inner = FunctionColumn.of("nullif", lit("x"), lit("y"));
        var outer = FunctionColumn.of("coalesce", func(inner), lit("fallback"));
        var sql = render(outer);
        assertEquals("coalesce(nullif('x', 'y'), 'fallback')", sql);
    }
}
