package io.sqm.render.ansi;

import io.sqm.core.Node;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FunctionExprRendererTest {

    /**
     * Helper to render a function column and return the SQL string.
     */
    private String render(Node node) {
        RenderContext ctx = RenderContext.of(new AnsiDialect());
        return ctx.render(node).sql();
    }

    @Test
    @DisplayName("Bare function with column arg and alias")
    void bareFunc_withColumnArg_andAlias() {
        var fc = func("lower", arg(col("t", "name"))).as("lname");
        var sql = render(fc);
        assertEquals("lower(t.name) AS lname", sql);
    }

    @Test
    @DisplayName("Schema-qualified function name")
    void qualifiedFuncName() {
        var fc = func("pg_catalog.lower", arg(col("name")));
        var sql = render(fc);
        assertEquals("pg_catalog.lower(name)", sql);
    }

    @Test
    @DisplayName("DISTINCT inside call with single column")
    void distinct_singleColumn() {
        var fc = func(
            "count",
            true,
            arg(col("t", "id")));
        var sql = render(fc);
        assertEquals("count(DISTINCT t.id)", sql);
    }

    @Test
    @DisplayName("COUNT(*) without alias")
    void count_star_noAlias() {
        var fc = func("count", starArg());
        var sql = render(fc);
        assertEquals("count(*)", sql);
    }

    @Test
    @DisplayName("ColumnRef without table")
    void columnRef_withoutTable() {
        var fc = func("upper", arg(col("city")));
        var sql = render(fc);
        assertEquals("upper(city)", sql);
    }

    @Test
    @DisplayName("ColumnRef with table")
    void columnRef_withTable() {
        var fc = func("upper", arg(col("addr", "city")));
        var sql = render(fc);
        assertEquals("upper(addr.city)", sql);
    }

    @Test
    @DisplayName("Literals: NULL, booleans, numbers, strings (escaping)")
    void literals_various() {
        var fc = func(
            "coalesce",
            arg(NULL),              // NULL
            arg(true),        // TRUE
            arg(42),          // number
            arg(3.14),        // decimal
            arg("O'Reilly")   // string with quote to be doubled
        );
        var sql = render(fc);
        assertEquals("coalesce(NULL, TRUE, 42, 3.14, 'O''Reilly')", sql);
    }

    @Test
    @DisplayName("String concatenation example with multiple literals")
    void concat_withLiterals() {
        var fc = func(
            "concat",
            arg("Hello"),
            arg(", "),
            arg("World")
        ).as("greeting");
        var sql = render(fc);
        assertEquals("concat('Hello', ', ', 'World') AS greeting", sql);
    }

    @Test
    @DisplayName("Function with no arguments renders empty parens")
    void noArguments() {
        var fc = func("random");
        var sql = render(fc);
        assertEquals("random()", sql);
    }

    @Test
    @DisplayName("DISTINCT with multiple args (generic function)")
    void distinct_multipleArgs() {
        var fc = func(
            "some_func",
            true,
            arg(col("t", "a")),
            arg(col("t", "b")));
        var sql = render(fc);
        // DISTINCT prefix is applied once before the full comma-separated list
        assertEquals("some_func(DISTINCT t.a, t.b)", sql);
    }

    @Test
    @DisplayName("Nested function as argument")
    void nestedFuncArg() {
        var inner = func("nullif", arg("x"), arg("y"));
        var outer = func("coalesce", arg(inner), arg("fallback"));
        var sql = render(outer);
        assertEquals("coalesce(nullif('x', 'y'), 'fallback')", sql);
    }
}
