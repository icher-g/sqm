package io.sqm.render.pgsql;

import io.sqm.core.*;
import io.sqm.render.pgsql.spi.PostgresDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PostgreSQL {@link ArraySubscriptExprRenderer}.
 */
@DisplayName("PostgreSQL ArraySubscriptExprRenderer Tests")
class ArraySubscriptExprRendererTest {

    private RenderContext renderContext;

    @BeforeEach
    void setUp() {
        renderContext = RenderContext.of(new PostgresDialect());
    }

    @Test
    @DisplayName("Render simple array subscript")
    void rendersSimpleArraySubscript() {
        var sub = ArraySubscriptExpr.of(col("arr"), lit(1));
        var sql = renderContext.render(sub).sql();
        
        assertEquals("arr[1]", normalizeWhitespace(sql));
    }

    @Test
    @DisplayName("Render array subscript with column index")
    void rendersArraySubscriptWithColumnIndex() {
        var sub = ArraySubscriptExpr.of(col("arr"), col("i"));
        var sql = renderContext.render(sub).sql();
        
        assertEquals("arr[i]", normalizeWhitespace(sql));
    }

    @Test
    @DisplayName("Render array subscript with expression index")
    void rendersArraySubscriptWithExpressionIndex() {
        var sub = ArraySubscriptExpr.of(col("arr"), col("i").add(lit(1)));
        var sql = renderContext.render(sub).sql();
        
        assertEquals("arr[i + 1]", normalizeWhitespace(sql));
    }

    @Test
    @DisplayName("Render chained array subscripts")
    void rendersChainedArraySubscripts() {
        var sub1 = ArraySubscriptExpr.of(col("matrix"), lit(1));
        var sub2 = ArraySubscriptExpr.of(sub1, lit(2));
        var sql = renderContext.render(sub2).sql();
        
        assertEquals("matrix[1][2]", normalizeWhitespace(sql));
    }

    @Test
    @DisplayName("Render three-level nested subscripts")
    void rendersThreeLevelSubscripts() {
        var sub1 = ArraySubscriptExpr.of(col("arr"), lit(1));
        var sub2 = ArraySubscriptExpr.of(sub1, lit(2));
        var sub3 = ArraySubscriptExpr.of(sub2, lit(3));
        var sql = renderContext.render(sub3).sql();
        
        assertEquals("arr[1][2][3]", normalizeWhitespace(sql));
    }

    @Test
    @DisplayName("Render array constructor with subscript")
    void rendersArrayConstructorWithSubscript() {
        var arr = array(lit(1), lit(2), lit(3));
        var sub = ArraySubscriptExpr.of(arr, lit(2));
        var sql = renderContext.render(sub).sql();
        
        assertEquals("ARRAY[1, 2, 3][2]", normalizeWhitespace(sql));
    }

    @Test
    @DisplayName("Render qualified column with subscript")
    void rendersQualifiedColumnWithSubscript() {
        var sub = ArraySubscriptExpr.of(col("t", "arr"), lit(1));
        var sql = renderContext.render(sub).sql();
        
        assertEquals("t.arr[1]", normalizeWhitespace(sql));
    }

    @Test
    @DisplayName("Render array subscript in SELECT list")
    void rendersArraySubscriptInSelectList() {
        var query = select(ArraySubscriptExpr.of(col("arr"), lit(1)).as("first_elem"))
            .from(tbl("t"));
        
        var sql = renderContext.render(query).sql();
        
        assertTrue(normalizeWhitespace(sql).contains("arr[1] AS first_elem"));
    }

    @Test
    @DisplayName("Render array subscript in WHERE clause")
    void rendersArraySubscriptInWhereClause() {
        var query = select(col("*"))
            .from(tbl("t"))
            .where(ArraySubscriptExpr.of(col("tags"), lit(1)).eq(lit("important")));
        
        var sql = renderContext.render(query).sql();
        
        assertTrue(normalizeWhitespace(sql).contains("tags[1] = 'important'"));
    }

    @Test
    @DisplayName("Render subscript with function call as base")
    void rendersSubscriptWithFunctionBase() {
        var func = func("string_to_array", arg(col("text")), arg(lit(",")));
        var sub = ArraySubscriptExpr.of(func, lit(1));
        var sql = renderContext.render(sub).sql();
        
        assertTrue(normalizeWhitespace(sql).contains("string_to_array(text, ',')[1]"));
    }

    @Test
    @DisplayName("Render negative index")
    void rendersNegativeIndex() {
        var sub = ArraySubscriptExpr.of(col("arr"), lit(-1));
        var sql = renderContext.render(sub).sql();
        
        assertEquals("arr[-1]", normalizeWhitespace(sql));
    }

    private String normalizeWhitespace(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
