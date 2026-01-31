package io.sqm.render.ansi;

import io.sqm.core.Node;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GroupByRenderer.
 */
class GroupByRendererTest {

    private final RenderContext ctx = RenderContext.of(new AnsiDialect());

    private String render(Node node) {
        return normalize(ctx.render(node).sql());
    }

    private String normalize(String s) {
        return s.replaceAll("\\s+", " ").trim();
    }

    @Test
    @DisplayName("GROUP BY single column")
    void group_by_single_column() {
        var query = select(col("dept"), func("count", arg(col("id"))))
            .from(tbl("employees"))
            .groupBy(group("dept"));
        String result = render(query);
        assertEquals("SELECT dept, count(id) FROM employees GROUP BY dept", result);
    }

    @Test
    @DisplayName("GROUP BY multiple columns")
    void group_by_multiple_columns() {
        var query = select(col("dept"), col("status"), func("count", arg(col("id"))))
            .from(tbl("employees"))
            .groupBy(group("dept"), group("status"));
        String result = render(query);
        assertTrue(result.contains("GROUP BY"));
        assertTrue(result.contains("dept"));
        assertTrue(result.contains("status"));
    }

    @Test
    @DisplayName("GROUP BY with qualified column")
    void group_by_qualified_column() {
        var query = select(col("e", "dept"), func("count", arg(col("id"))))
            .from(tbl("employees").as("e"))
            .groupBy(group("e", "dept"));
        String result = render(query);
        assertTrue(result.contains("GROUP BY"));
        assertTrue(result.contains("e.dept"));
    }

    @Test
    @DisplayName("GROUP BY renders with keyword")
    void group_by_keyword_rendered() {
        var query = select(col("category"), func("count", arg(col("id"))))
            .from(tbl("products"))
            .groupBy(group("category"));
        String result = render(query);
        assertTrue(result.contains("GROUP BY"));
    }

    @Test
    @DisplayName("GROUP BY with aggregate and HAVING")
    void group_by_with_having() {
        var query = select(col("dept"), func("count", arg(col("id"))).as("cnt"))
            .from(tbl("employees"))
            .groupBy(group("dept"))
            .having(func("count", arg(col("id"))).gt(5));
        String result = render(query);
        assertTrue(result.contains("GROUP BY"));
        assertTrue(result.contains("HAVING"));
    }

    @Test
    @DisplayName("GROUP BY with ROLLUP is rejected in ANSI renderer")
    void group_by_rollup_rejected() {
        var query = select(col("dept"), func("count", arg(col("id"))))
            .from(tbl("employees"))
            .groupBy(rollup(group("dept"), group("status")));

        assertThrows(UnsupportedOperationException.class, () -> render(query));
    }

    @Test
    @DisplayName("GROUP BY with GROUPING SETS is rejected in ANSI renderer")
    void group_by_grouping_sets_rejected() {
        var query = select(col("dept"), func("count", arg(col("id"))))
            .from(tbl("employees"))
            .groupBy(groupingSets(group("dept"), groupingSet()));

        assertThrows(UnsupportedOperationException.class, () -> render(query));
    }

    @Test
    @DisplayName("GROUP BY with grouping set is rejected in ANSI renderer")
    void group_by_grouping_set_rejected() {
        var query = select(col("dept"), func("count", arg(col("id"))))
            .from(tbl("employees"))
            .groupBy(groupingSet(group("dept")));

        assertThrows(UnsupportedOperationException.class, () -> render(query));
    }

    @Test
    @DisplayName("GROUP BY with CUBE is rejected in ANSI renderer")
    void group_by_cube_rejected() {
        var query = select(col("dept"), func("count", arg(col("id"))))
            .from(tbl("employees"))
            .groupBy(cube(group("dept"), group("status")));

        assertThrows(UnsupportedOperationException.class, () -> render(query));
    }
}
