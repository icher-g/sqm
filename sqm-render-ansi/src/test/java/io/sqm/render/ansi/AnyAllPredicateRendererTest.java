package io.sqm.render.ansi;

import io.sqm.core.AnyAllPredicate;
import io.sqm.core.ComparisonOperator;
import io.sqm.core.Node;
import io.sqm.core.Quantifier;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnyAllPredicateRendererTest {

    private final RenderContext ctx = RenderContext.of(new AnsiDialect());

    private String render(Node node) {
        return normalize(ctx.render(node).sql());
    }

    private String normalize(String s) {
        return s.replaceAll("\\s+", " ").trim();
    }

    @Test
    @DisplayName("ANY quantifier renders ANY with subquery")
    void rendersAny() {
        var subquery = select(lit(1)).from(tbl("nums")).build();
        var predicate = AnyAllPredicate.of(col("a"), ComparisonOperator.EQ, subquery, Quantifier.ANY);

        String sql = render(predicate);
        assertTrue(sql.contains("a = ANY"));
        assertTrue(sql.contains("SELECT 1"));
        assertTrue(sql.contains("FROM nums"));
    }

    @Test
    @DisplayName("ALL quantifier renders ALL with subquery")
    void rendersAll() {
        var subquery = select(lit(1)).from(tbl("nums")).build();
        var predicate = AnyAllPredicate.of(col("a"), ComparisonOperator.EQ, subquery, Quantifier.ALL);

        String sql = render(predicate);
        assertTrue(sql.contains("a = ALL"));
        assertTrue(sql.contains("SELECT 1"));
        assertTrue(sql.contains("FROM nums"));
    }
}
