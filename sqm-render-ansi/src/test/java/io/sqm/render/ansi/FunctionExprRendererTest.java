package io.sqm.render.ansi;

import io.sqm.core.FunctionExpr;
import io.sqm.core.Identifier;
import io.sqm.core.QualifiedName;
import io.sqm.core.QuoteStyle;
import io.sqm.core.Node;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FunctionExprRendererTest {

    static String norm(String s) {
        return s.replaceAll("\\s+", " ").trim();
    }

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
    @DisplayName("Qualified function name preserves or falls back quote style per part")
    void qualifiedFuncName_quoteAware() {
        var preserved = FunctionExpr.of(
            new QualifiedName(java.util.List.of(
                Identifier.of("pg_catalog"),
                Identifier.of("Lower", QuoteStyle.DOUBLE_QUOTE)
            )),
            java.util.List.of(FunctionExpr.Arg.expr(col("name"))),
            null,
            null,
            null,
            null
        );
        assertEquals("pg_catalog.\"Lower\"(name)", render(preserved));

        var fallback = FunctionExpr.of(
            new QualifiedName(java.util.List.of(
                Identifier.of("pg_catalog"),
                Identifier.of("Lower", QuoteStyle.BACKTICK)
            )),
            java.util.List.of(FunctionExpr.Arg.expr(col("name"))),
            null,
            null,
            null,
            null
        );
        assertEquals("pg_catalog.\"Lower\"(name)", render(fallback));
    }

    @Test
    @DisplayName("DISTINCT inside call with single column")
    void distinct_singleColumn() {
        var fc = func(
            "count",
            arg(col("t", "id"))).distinct();
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
            arg(lit(NULL)),              // NULL
            arg(lit(true)),        // TRUE
            arg(lit(42)),          // number
            arg(lit(3.14)),        // decimal
            arg(lit("O'Reilly"))   // string with quote to be doubled
        );
        var sql = render(fc);
        assertEquals("coalesce(NULL, TRUE, 42, 3.14, 'O''Reilly')", sql);
    }

    @Test
    @DisplayName("String concatenation example with multiple literals")
    void concat_withLiterals() {
        var fc = func(
            "concat",
            arg(lit("Hello")),
            arg(lit(", ")),
            arg(lit("World"))
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
            arg(col("t", "a")),
            arg(col("t", "b"))).distinct();
        var sql = render(fc);
        // DISTINCT prefix is applied once before the full comma-separated list
        assertEquals("some_func(DISTINCT t.a, t.b)", sql);
    }

    @Test
    @DisplayName("Nested function as argument")
    void nestedFuncArg() {
        var inner = func("nullif", arg(lit("x")), arg(lit("y")));
        var outer = func("coalesce", arg(inner), arg(lit("fallback")));
        var sql = render(outer);
        assertEquals("coalesce(nullif('x', 'y'), 'fallback')", sql);
    }

    @Test
    void lower_of_case_expr_renders_correctly() {
        var q = select(
            func("lower", arg(
                kase(when(col("u", "flag").gt(0)).then(col("u", "name")))
            )).as("ln")
        ).from(tbl("users").as("u")).build();

        String sql = render(q);
        assertTrue(sql.toLowerCase().contains("lower(case when u.flag > 0 then u.name end)"),
            "Expected LOWER(CASE WHEN u.flag > 0 THEN u.name END), got: " + sql);
    }

    @Test
    void coalesce_with_scalar_subquery_argument_renders() {
        var sub = select(func("max", arg(col("t", "v")))).from(tbl("t")).build();

        var q = select(
            func("coalesce", arg(expr(sub)), arg(lit(0))).as("mx")
        ).from(tbl("dual")).build();

        String sql = norm(render(q).stripIndent().toLowerCase());
        assertTrue(sql.matches(".*coalesce\\s*\\(\\s*\\(\\s*select\\b.*max\\s*\\(t\\.v\\)\\s*from\\s*t\\s*\\)\\s*,\\s*0\\s*\\).*"),
            "Expected COALESCE((SELECT MAX(t.v) FROM t), 0), got: " + sql);
    }

    @Test
    void count_star_renders() {
        var q = select(
            func("count", starArg()).as("cnt")
        ).from(tbl("users")).build();

        String sql = norm(render(q)).toLowerCase();
        assertTrue(sql.contains("count(*)"), "Expected COUNT(*), got: " + sql);
    }

    @Test
    void coalesce_lower_col_and_literal_renders() {
        var q = select(
            func("coalesce",
                arg(func("lower", arg(col("u", "name")))),
                arg(lit("N/A"))
            ).as("val")
        ).from(tbl("users").as("u")).build();

        String sql = norm(render(q)).toLowerCase();
        assertTrue(sql.contains("coalesce(lower(u.name), 'n/a')"),
            "Expected COALESCE(LOWER(u.name), 'N/A'), got: " + sql);
    }
}

