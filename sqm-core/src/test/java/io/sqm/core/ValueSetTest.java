package io.sqm.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValueSetTest {

    @Test
    void asQuery() {
        ValueSet query = Expression.subquery(Query.select(Expression.literal(1)));
        assertTrue(query.asQuery().isPresent());
        assertFalse(Expression.row(1, 2, 3).asQuery().isPresent());
    }

    @Test
    void asRow() {
        ValueSet row = Expression.row(1, 2, 3);
        assertTrue(row.asRow().isPresent());
        assertFalse(Expression.subquery(Query.select(Expression.literal(1))).asRow().isPresent());
    }

    @Test
    void asRows() {
        ValueSet rows = Expression.rows(Expression.row(1, 2, 3));
        assertTrue(rows.asRows().isPresent());
        assertFalse(Expression.row(1, 2, 3).asRows().isPresent());
    }
}