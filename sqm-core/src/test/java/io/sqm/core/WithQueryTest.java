package io.sqm.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WithQueryTest {

    @Test
    void of() {
        var query = Query.select(Expression.literal(1));
        var cte = CteDef.of("cte", query);
        var with = WithQuery.of(List.of(cte), query, true);
        assertEquals("cte", with.ctes().getFirst().name());
        assertTrue(with.recursive());
    }

    @Test
    void recursive() {
        var query = Query.select(Expression.literal(1));
        var cte = CteDef.of("cte", query);
        var with = WithQuery.of(List.of(cte), query);
        assertFalse(with.recursive());
        with = with.recursive(true);
        assertTrue(with.recursive());
    }

    @Test
    void body() {
        var query = Query.select(Expression.literal(1));
        var cte = CteDef.of("cte", query);
        var with = WithQuery.of(cte);
        assertNull(with.body());
        with = with.body(query);
        assertNotNull(with.body());
    }
}