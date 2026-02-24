package io.sqm.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValuesTableTest {

    @Test
    void as() {
        var table = ValuesTable.of(RowListExpr.of(List.of(RowExpr.of(List.of(LiteralExpr.of(1))))));
        assertEquals("tableAlias", table.as("tableAlias").alias().value());
    }

    @Test
    void columnAliases() {
        var table = ValuesTable.of(RowListExpr.of(List.of(RowExpr.of(List.of(LiteralExpr.of(1))))));
        assertEquals(2, table.columnAliases("c1", "c2").columnAliases().size());
        assertEquals(2, table.columnAliases("c1", "c2").columnAliases().size());
    }
}
