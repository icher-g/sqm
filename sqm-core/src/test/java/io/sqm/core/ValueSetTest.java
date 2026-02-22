package io.sqm.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValueSetTest {

    @Test
    void maybeQuery() {
        ValueSet query = Expression.subquery(Query.select(Expression.literal(1)).build());
        assertTrue(query.<Boolean>matchValueSet().query(s -> true).orElse(false));
        assertFalse(Expression.row(1, 2, 3).<Boolean>matchValueSet().query(s -> true).orElse(false));
    }

    @Test
    void maybeRow() {
        ValueSet row = Expression.row(1, 2, 3);
        assertTrue(row.<Boolean>matchValueSet().row(s -> true).orElse(false));
        assertFalse(Expression.subquery(Query.select(Expression.literal(1)).build()).<Boolean>matchValueSet().row(s -> true).orElse(false));
    }

    @Test
    void maybeRows() {
        ValueSet rows = Expression.rows(Expression.row(1, 2, 3));
        assertTrue(rows.<Boolean>matchValueSet().rows(s -> true).orElse(false));
        assertFalse(Expression.row(1, 2, 3).<Boolean>matchValueSet().rows(s -> true).orElse(false));
    }
}