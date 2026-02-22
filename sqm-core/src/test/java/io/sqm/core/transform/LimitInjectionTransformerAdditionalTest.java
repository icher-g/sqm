package io.sqm.core.transform;

import io.sqm.core.CompositeQuery;
import io.sqm.core.LiteralExpr;
import io.sqm.core.SelectQuery;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.select;
import static io.sqm.dsl.Dsl.tbl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class LimitInjectionTransformerAdditionalTest {

    @Test
    void expression_based_default_limit_is_used() {
        SelectQuery query = select(col("id")).from(tbl("users")).build();

        var transformed = (SelectQuery) LimitInjectionTransformer.of(lit(7)).apply(query);

        assertEquals(7, ((LiteralExpr) transformed.limitOffset().limit()).value());
    }

    @Test
    void composite_with_explicit_limit_is_unchanged() {
        CompositeQuery query = select(lit(1)).build().union(select(lit(2)).build()).limit(3L);

        var transformed = (CompositeQuery) LimitInjectionTransformer.of(50).apply(query);

        assertSame(query, transformed);
        assertEquals(3L, ((LiteralExpr) transformed.limitOffset().limit()).value());
    }
}
