package io.sqm.render.postgresql;

import io.sqm.core.ArrayExpr;
import io.sqm.render.postgresql.spi.PostgresDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Render tests for array expressions using the PostgreSQL dialect.
 */
@DisplayName("PostgreSQL ArrayExpr Rendering")
class ArrayExprRenderTest {

    private RenderContext renderContext;

    @BeforeEach
    void setUp() {
        renderContext = RenderContext.of(new PostgresDialect());
    }

    @Test
    @DisplayName("Render empty array")
    void rendersEmptyArray() {
        var arr = ArrayExpr.EMPTY;
        var sql = renderContext.render(arr).sql();

        assertEquals("ARRAY[]", normalizeWhitespace(sql));
    }

    @Test
    @DisplayName("Render array with literals")
    void rendersArrayWithLiterals() {
        var arr = array(lit(1), lit(2), lit(3));
        var sql = renderContext.render(arr).sql();

        assertEquals("ARRAY[1, 2, 3]", normalizeWhitespace(sql));
    }

    @Test
    @DisplayName("Render array with string literals")
    void rendersArrayWithStringLiterals() {
        var arr = array(lit("a"), lit("b"), lit("c"));
        var sql = renderContext.render(arr).sql();

        assertEquals("ARRAY['a', 'b', 'c']", normalizeWhitespace(sql));
    }

    @Test
    @DisplayName("Render array with columns")
    void rendersArrayWithColumns() {
        var arr = array(col("a"), col("b"), col("c"));
        var sql = renderContext.render(arr).sql();

        assertEquals("ARRAY[a, b, c]", normalizeWhitespace(sql));
    }

    @Test
    @DisplayName("Render nested arrays")
    void rendersNestedArrays() {
        var inner1 = array(lit(1), lit(2));
        var inner2 = array(lit(3), lit(4));
        var outer = array(inner1, inner2);
        var sql = renderContext.render(outer).sql();

        assertTrue(normalizeWhitespace(sql).contains("ARRAY[ARRAY[1, 2], ARRAY[3, 4]]"));
    }

    @Test
    @DisplayName("Render array in SELECT list")
    void rendersArrayInSelectList() {
        var query = select(array(lit(1), lit(2), lit(3)).as("nums"))
            .from(tbl("t"));

        var sql = renderContext.render(query).sql();

        assertTrue(normalizeWhitespace(sql).contains("ARRAY[1, 2, 3] AS nums"));
    }

    @Test
    @DisplayName("Render array in WHERE clause")
    void rendersArrayInWhereClause() {
        var query = select(col("*"))
            .from(tbl("t"))
            .where(col("tags").eq(array(lit("tag1"), lit("tag2"))));

        var sql = renderContext.render(query).sql();

        assertTrue(normalizeWhitespace(sql).contains("ARRAY['tag1', 'tag2']"));
    }

    @Test
    @DisplayName("Render array with function calls")
    void rendersArrayWithFunctions() {
        var arr = array(
            func("LOWER", arg(col("a"))),
            func("UPPER", arg(col("b")))
        );
        var sql = renderContext.render(arr).sql();

        assertTrue(normalizeWhitespace(sql).contains("ARRAY["));
        assertTrue(normalizeWhitespace(sql).contains("LOWER(a)"));
        assertTrue(normalizeWhitespace(sql).contains("UPPER(b)"));
    }

    @Test
    @DisplayName("Render array with expressions")
    void rendersArrayWithExpressions() {
        var arr = array(
            col("a").add(lit(1)),
            col("b").mul(lit(2))
        );
        var sql = renderContext.render(arr).sql();

        assertTrue(normalizeWhitespace(sql).contains("ARRAY["));
    }

    private String normalizeWhitespace(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
