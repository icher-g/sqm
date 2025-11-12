package io.sqm.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

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
}