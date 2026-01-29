package io.sqm.render.postgresql;

import io.sqm.core.*;
import io.sqm.render.postgresql.spi.PostgresDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PostgreSQL {@link ArraySliceExprRenderer}.
 */
@DisplayName("PostgreSQL ArraySliceExprRenderer Tests")
class ArraySliceExprRendererTest {

    private RenderContext renderContext;

    @BeforeEach
    void setUp() {
        renderContext = RenderContext.of(new PostgresDialect());
    }

    @Test
    @DisplayName("Render array slice with both bounds")
    void rendersArraySliceWithBothBounds() {
        var slice = ArraySliceExpr.of(col("arr"), lit(2), lit(5));
        var sql = renderContext.render(slice).sql();
        
        assertEquals("arr[2:5]", normalizeWhitespace(sql));
    }

    @Test
    @DisplayName("Render array slice with missing from bound")
    void rendersArraySliceWithMissingFrom() {
        var slice = ArraySliceExpr.of(col("arr"), null, lit(5));
        var sql = renderContext.render(slice).sql();
        
        assertEquals("arr[:5]", normalizeWhitespace(sql));
    }

    @Test
    @DisplayName("Render array slice with missing to bound")
    void rendersArraySliceWithMissingTo() {
        var slice = ArraySliceExpr.of(col("arr"), lit(2), null);
        var sql = renderContext.render(slice).sql();
        
        assertEquals("arr[2:]", normalizeWhitespace(sql));
    }

    @Test
    @DisplayName("Render array slice with both bounds missing")
    void rendersArraySliceWithBothBoundsMissing() {
        var slice = ArraySliceExpr.of(col("arr"), null, null);
        var sql = renderContext.render(slice).sql();
        
        assertEquals("arr[:]", normalizeWhitespace(sql));
    }

    @Test
    @DisplayName("Render array slice with column bounds")
    void rendersArraySliceWithColumnBounds() {
        var slice = ArraySliceExpr.of(col("arr"), col("start_idx"), col("end_idx"));
        var sql = renderContext.render(slice).sql();
        
        assertEquals("arr[start_idx:end_idx]", normalizeWhitespace(sql));
    }

    @Test
    @DisplayName("Render array slice with expression bounds")
    void rendersArraySliceWithExpressionBounds() {
        var from = col("i").add(lit(1));
        var to = col("i").add(lit(5));
        var slice = ArraySliceExpr.of(col("arr"), from, to);
        var sql = renderContext.render(slice).sql();
        
        assertEquals("arr[i + 1:i + 5]", normalizeWhitespace(sql));
    }

    @Test
    @DisplayName("Render qualified column with slice")
    void rendersQualifiedColumnWithSlice() {
        var slice = ArraySliceExpr.of(col("t", "arr"), lit(2), lit(5));
        var sql = renderContext.render(slice).sql();
        
        assertEquals("t.arr[2:5]", normalizeWhitespace(sql));
    }

    @Test
    @DisplayName("Render array constructor with slice")
    void rendersArrayConstructorWithSlice() {
        var arr = array(lit(1), lit(2), lit(3), lit(4), lit(5));
        var slice = ArraySliceExpr.of(arr, lit(2), lit(4));
        var sql = renderContext.render(slice).sql();
        
        assertEquals("ARRAY[1, 2, 3, 4, 5][2:4]", normalizeWhitespace(sql));
    }

    @Test
    @DisplayName("Render array slice in SELECT list")
    void rendersArraySliceInSelectList() {
        var query = select(ArraySliceExpr.of(col("arr"), lit(1), lit(3)).as("slice"))
            .from(tbl("t"));
        
        var sql = renderContext.render(query).sql();
        
        assertTrue(normalizeWhitespace(sql).contains("arr[1:3] AS slice"));
    }

    @Test
    @DisplayName("Render array slice in WHERE clause")
    void rendersArraySliceInWhereClause() {
        var arr = array(lit("a"), lit("b"), lit("c"));
        var query = select(col("*"))
            .from(tbl("t"))
            .where(ArraySliceExpr.of(col("tags"), lit(1), lit(3)).eq(arr));
        
        var sql = renderContext.render(query).sql();
        
        assertTrue(normalizeWhitespace(sql).contains("tags[1:3] = ARRAY['a', 'b', 'c']"));
    }

    @Test
    @DisplayName("Render slice with function call as base")
    void rendersSliceWithFunctionBase() {
        var func = func("string_to_array", arg(col("text")), arg(lit(",")));
        var slice = ArraySliceExpr.of(func, lit(1), lit(3));
        var sql = renderContext.render(slice).sql();
        
        assertTrue(normalizeWhitespace(sql).contains("string_to_array(text, ',')[1:3]"));
    }

    @Test
    @DisplayName("Render negative bounds")
    void rendersNegativeBounds() {
        var slice = ArraySliceExpr.of(col("arr"), lit(-3), lit(-1));
        var sql = renderContext.render(slice).sql();
        
        assertEquals("arr[-3:-1]", normalizeWhitespace(sql));
    }

    private String normalizeWhitespace(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
