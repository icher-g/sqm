package io.sqm.core.transform;

import io.sqm.core.ParamExpr;
import io.sqm.core.Query;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.select;
import static io.sqm.dsl.Dsl.tbl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class LiteralTransformsTest {

    @Test
    void parameterizeRewritesLiteralsToOrdinalParamsAndCollectsValues() {
        Query query = select(col("id"))
            .from(tbl("users"))
            .where(col("tenant_id").eq(lit(42)).and(col("active").eq(lit(true))))
            .build();

        var parameterized = LiteralTransforms.parameterize(query);

        assertNotSame(query, parameterized.node());
        assertEquals(2, parameterized.values().size());
        assertEquals(42, parameterized.values().get(0));
        assertEquals(true, parameterized.values().get(1));
        assertEquals(2, parameterized.valuesByParam().size());
    }

    @Test
    void parameterizeSupportsCustomParameterFactory() {
        Query query = select(lit("a"), lit("b")).from(tbl("users")).build();

        var parameterized = LiteralTransforms.parameterize(query, i -> ParamExpr.named("p" + i));

        var select = assertInstanceOf(io.sqm.core.SelectQuery.class, parameterized.node());
        assertEquals("a", parameterized.values().get(0));
        assertEquals("b", parameterized.values().get(1));
        assertEquals(
            "p1",
            select.items().getFirst().matchSelectItem()
                .expr(item -> item.expr().matchExpression().param(param -> param.matchParam().named(named -> named.name()).orElse(null)).orElse(null))
                .orElse(null)
        );
    }

    @Test
    void normalizeLiteralsMatchesParameterizedNodeShape() {
        Query query = select(lit(7)).from(tbl("users")).where(col("id").eq(lit(1))).build();

        Query normalized = LiteralTransforms.normalizeLiterals(query);
        Query parameterizedNode = LiteralTransforms.parameterize(query).node();

        assertEquals(parameterizedNode, normalized);
    }

    @Test
    void normalizeLiteralsPreservesIdentityWhenNoLiteralsExist() {
        Query query = select(col("id")).from(tbl("users")).where(col("active").eq(col("enabled"))).build();

        Query normalized = LiteralTransforms.normalizeLiterals(query);

        assertSame(query, normalized);
    }
}
