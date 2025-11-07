package io.sqm.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CteDefTest {

    @Test
    void of() {
        var cte = CteDef.of("name", Query.select(Expression.literal(1)), List.of("c1"));
        assertEquals("name", cte.name());
        assertEquals(1, cte.body().asSelect().orElseThrow().select().get(0).asExpr().orElseThrow().expr().asLiteral().orElseThrow().value());
        assertEquals("c1", cte.columnAliases().get(0));
    }

    @Test
    void body() {
        var body = Query.select(Expression.literal(1));
        var cte = CteDef.of("name").body(body);
        assertEquals("name", cte.name());
        assertEquals(1, cte.body().asSelect().orElseThrow().select().get(0).asExpr().orElseThrow().expr().asLiteral().orElseThrow().value());
        assertNull(cte.columnAliases());
    }

    @Test
    void columnAliases() {
        var aliases = List.of("c1");
        var cte = CteDef.of("name").columnAliases(aliases);
        assertEquals("name", cte.name());
        assertNull(cte.body());
        assertEquals("c1", cte.columnAliases().get(0));
        cte = CteDef.of("name").columnAliases("c2");
        assertEquals("name", cte.name());
        assertNull(cte.body());
        assertEquals("c2", cte.columnAliases().get(0));
    }
}