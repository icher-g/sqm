package io.sqm.render.postgresql;

import io.sqm.core.Node;
import io.sqm.render.postgresql.spi.PostgresDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("PostgreSQL GroupByRenderer Tests")
class GroupByRendererTest {

    private final RenderContext ctx = RenderContext.of(new PostgresDialect());

    private String render(Node node) {
        return normalize(ctx.render(node).sql());
    }

    private String normalize(String s) {
        return s.replaceAll("\\s+", " ").trim();
    }

    @Test
    @DisplayName("Render GROUPING SETS")
    void rendersGroupingSets() {
        var query = select(col("a"), func("count", arg(col("b"))))
            .from(tbl("t"))
            .groupBy(groupingSets(
                group("a"),
                groupingSet(group("a"), group("b")),
                groupingSet()
            ));

        String result = render(query);
        assertEquals("SELECT a, count(b) FROM t GROUP BY GROUPING SETS (a, (a, b), ())", result);
    }

    @Test
    @DisplayName("Render ROLLUP")
    void rendersRollup() {
        var query = select(col("a"), func("count", arg(col("b"))))
            .from(tbl("t"))
            .groupBy(rollup(group("a"), group("b")));

        String result = render(query);
        assertEquals("SELECT a, count(b) FROM t GROUP BY ROLLUP (a, b)", result);
    }

    @Test
    @DisplayName("Render CUBE")
    void rendersCube() {
        var query = select(col("a"), func("count", arg(col("b"))))
            .from(tbl("t"))
            .groupBy(cube(group("a"), group("b")));

        String result = render(query);
        assertEquals("SELECT a, count(b) FROM t GROUP BY CUBE (a, b)", result);
    }
}
