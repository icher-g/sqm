package io.sqm.render.postgresql;

import io.sqm.render.postgresql.spi.PostgresDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Render tests for array subscripts and slices using the PostgreSQL dialect.
 */
@DisplayName("PostgreSQL Array Subscript/Slice Rendering")
class ArraySubscriptSliceRenderTest {

    private RenderContext renderContext;

    @BeforeEach
    void setUp() {
        renderContext = RenderContext.of(new PostgresDialect());
    }

    @Test
    @DisplayName("Render array subscript on array literal")
    void rendersArraySubscriptOnArrayLiteral() {
        var expr = array(lit(1), lit(2), lit(3)).at(2);
        var sql = renderContext.render(expr).sql();

        assertEquals("ARRAY[1, 2, 3][2]", normalizeWhitespace(sql));
    }

    @Test
    @DisplayName("Render array subscript on column")
    void rendersArraySubscriptOnColumn() {
        var expr = col("tags").at(1);
        var sql = renderContext.render(expr).sql();

        assertEquals("tags[1]", normalizeWhitespace(sql));
    }

    @Test
    @DisplayName("Render nested array subscript")
    void rendersNestedArraySubscript() {
        var expr = array(lit(1), lit(2), lit(3)).at(1).at(2);
        var sql = renderContext.render(expr).sql();

        assertEquals("ARRAY[1, 2, 3][1][2]", normalizeWhitespace(sql));
    }

    @Test
    @DisplayName("Render array slice with bounds")
    void rendersArraySliceWithBounds() {
        var expr = col("nums").slice(2, 4);
        var sql = renderContext.render(expr).sql();

        assertEquals("nums[2:4]", normalizeWhitespace(sql));
    }

    @Test
    @DisplayName("Render array slice from bound")
    void rendersArraySliceFromBound() {
        var expr = col("nums").sliceFrom(2);
        var sql = renderContext.render(expr).sql();

        assertEquals("nums[2:]", normalizeWhitespace(sql));
    }

    @Test
    @DisplayName("Render array slice to bound")
    void rendersArraySliceToBound() {
        var expr = col("nums").sliceTo(3);
        var sql = renderContext.render(expr).sql();

        assertEquals("nums[:3]", normalizeWhitespace(sql));
    }

    private String normalizeWhitespace(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
