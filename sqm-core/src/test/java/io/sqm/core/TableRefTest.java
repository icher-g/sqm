package io.sqm.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TableRefTest {

    @Test
    void table() {
        assertInstanceOf(Table.class, TableRef.table("t"));
        assertInstanceOf(Table.class, TableRef.table("sys", "t"));
    }

    @Test
    void query() {
        assertInstanceOf(QueryTable.class, TableRef.query(Query.select(Expression.literal(1))));
    }

    @Test
    void values() {
        assertInstanceOf(ValuesTable.class, TableRef.values(Expression.rows(Expression.row(1, 2, 3))));
    }

    @Test
    void alias() {
        var table = TableRef.table("t").as("a");
        assertEquals("a", table.alias());
    }

    @Test
    void asTable() {
        TableRef table = TableRef.table("t");
        assertTrue(table.asTable().isPresent());
        assertFalse(TableRef.query(Query.select(Expression.literal(1))).asTable().isPresent());
    }

    @Test
    void asQuery() {
        TableRef query = TableRef.query(Query.select(Expression.literal(1)));
        assertTrue(query.asQuery().isPresent());
        assertFalse(TableRef.table("t").asQuery().isPresent());
    }

    @Test
    void asValues() {
        TableRef values = TableRef.values(Expression.rows(Expression.row(1, 2, 3)));
        assertTrue(values.asValues().isPresent());
        assertFalse(TableRef.table("t").asValues().isPresent());
    }
}