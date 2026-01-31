package io.sqm.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CteDefTest {

    @Test
    void of() {
        var cte = CteDef.of("name", Query.select(Expression.literal(1)), List.of("c1"));
        assertEquals("name", cte.name());
        assertEquals(1, cte.body().matchQuery()
            .select(s -> s.items().getFirst().matchSelectItem()
                .expr(e -> e.expr().matchExpression()
                    .literal(l -> l.value())
                    .orElse(null)
                )
                .orElse(null)
            )
            .orElse(null)
        );
        assertEquals("c1", cte.columnAliases().getFirst());
        assertEquals(CteDef.Materialization.DEFAULT, cte.materialization());
    }

    @Test
    void body() {
        var body = Query.select(Expression.literal(1));
        var cte = CteDef.of("name").body(body);
        assertEquals("name", cte.name());
        assertEquals(1, cte.body().matchQuery()
            .select(s -> s.items().getFirst().matchSelectItem()
                .expr(e -> e.expr().matchExpression()
                    .literal(l -> l.value())
                    .orElse(null)
                )
                .orElse(null)
            )
            .orElse(null)
        );
        assertNull(cte.columnAliases());
        assertEquals(CteDef.Materialization.DEFAULT, cte.materialization());
    }

    @Test
    void columnAliases() {
        var aliases = List.of("c1");
        var cte = CteDef.of("name").columnAliases(aliases);
        assertEquals("name", cte.name());
        assertNull(cte.body());
        assertEquals("c1", cte.columnAliases().getFirst());
        assertEquals(CteDef.Materialization.DEFAULT, cte.materialization());
        cte = CteDef.of("name").columnAliases("c2");
        assertEquals("name", cte.name());
        assertNull(cte.body());
        assertEquals("c2", cte.columnAliases().getFirst());
        assertEquals(CteDef.Materialization.DEFAULT, cte.materialization());
    }

    @Test
    void materialization() {
        var cte = CteDef.of("name", Query.select(Expression.literal(1)), List.of(), CteDef.Materialization.MATERIALIZED);
        assertEquals(CteDef.Materialization.MATERIALIZED, cte.materialization());
        cte = cte.materialization(CteDef.Materialization.NOT_MATERIALIZED);
        assertEquals(CteDef.Materialization.NOT_MATERIALIZED, cte.materialization());
    }

    @Test
    void dsl_materialization_factory() {
        var q = Query.select(Expression.literal(1));
        var cte = io.sqm.dsl.Dsl.cte("name", q, List.of("c1"), CteDef.Materialization.MATERIALIZED);
        assertEquals(CteDef.Materialization.MATERIALIZED, cte.materialization());
        assertEquals("c1", cte.columnAliases().getFirst());
    }
}
