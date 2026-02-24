package io.sqm.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FunctionTable Tests")
class FunctionTableTest {

    @Test
    @DisplayName("Create function table without alias")
    void createWithoutAlias() {
        var func = func("generate_series", arg(lit(1)), arg(lit(10)));
        var table = FunctionTable.of(func);

        assertNotNull(table);
        assertEquals(func, table.function());
        assertNull(table.alias());
        assertTrue(table.columnAliases().isEmpty());
        assertFalse(table.ordinality());
    }

    @Test
    @DisplayName("Create function table with alias")
    void createWithAlias() {
        var func = func("unnest", arg(array(lit(1), lit(2), lit(3))));
        var table = FunctionTable.of(func).as("t");

        assertNotNull(table);
        assertEquals(func, table.function());
        assertEquals("t", table.alias().value());
        assertTrue(table.columnAliases().isEmpty());
        assertFalse(table.ordinality());
    }

    @Test
    @DisplayName("Create function table with alias and column aliases")
    void createWithAliasAndColumns() {
        var func = func("json_to_record", arg(lit("{}")));
        var table = FunctionTable.of(func)
            .as("t")
            .columnAliases("id", "name");

        assertNotNull(table);
        assertEquals(func, table.function());
        assertEquals("t", table.alias().value());
        assertEquals(2, table.columnAliases().size());
        assertTrue(table.columnAliases().stream().map(Identifier::value).toList().contains("id"));
        assertTrue(table.columnAliases().stream().map(Identifier::value).toList().contains("name"));
        assertFalse(table.ordinality());
    }

    @Test
    @DisplayName("Add alias to function table")
    void addAlias() {
        var func = func("generate_series", arg(lit(1)), arg(lit(10)));
        var table = FunctionTable.of(func).as("series");

        assertEquals("series", table.alias().value());
        assertEquals(func, table.function());
    }

    @Test
    @DisplayName("Enable WITH ORDINALITY")
    void enableWithOrdinality() {
        var func = func("generate_series", arg(lit(1)), arg(lit(10)));
        var table = FunctionTable.of(func).withOrdinality();

        assertTrue(table.ordinality());
        assertEquals(func, table.function());
    }

    @Test
    @DisplayName("Add column aliases as list")
    void addColumnAliasesList() {
        var func = func("generate_series", arg(lit(1)), arg(lit(10)));
        var table = FunctionTable.of(func).as("t")
            .columnAliases("num");

        assertEquals(1, table.columnAliases().size());
        assertEquals("num", table.columnAliases().getFirst().value());
    }

    @Test
    @DisplayName("Add column aliases as varargs")
    void addColumnAliasesVarargs() {
        var func = func("json_each", arg(col("data")));
        var table = FunctionTable.of(func).as("t")
            .columnAliases("key", "value");

        assertEquals(2, table.columnAliases().size());
        assertEquals("key", table.columnAliases().get(0).value());
        assertEquals("value", table.columnAliases().get(1).value());
    }

    @Test
    @DisplayName("Function expression asTable() method")
    void functionExprAsTable() {
        var func = func("generate_series", arg(lit(1)), arg(lit(10)));
        var table = func.asTable();

        assertNotNull(table);
        assertEquals(func, table.function());
        assertNull(table.alias());
    }

    @Test
    @DisplayName("Chaining alias and column aliases")
    void chainingAliasAndColumns() {
        var func = func("generate_series", arg(lit(1)), arg(lit(10)));
        var table = FunctionTable.of(func)
            .as("series")
            .columnAliases("num");

        assertEquals("series", table.alias().value());
        assertEquals(1, table.columnAliases().size());
        assertEquals("num", table.columnAliases().getFirst().value());
    }

    @Test
    @DisplayName("FunctionTable is immutable")
    void immutability() {
        var func = func("generate_series", arg(lit(1)), arg(lit(10)));
        var table1 = FunctionTable.of(func);
        var table2 = table1.as("t");
        var table3 = table2.columnAliases("num");
        var table4 = table3.withOrdinality();

        assertNull(table1.alias());
        assertEquals("t", table2.alias().value());
        assertEquals("t", table3.alias().value());
        assertTrue(table1.columnAliases().isEmpty());
        assertTrue(table2.columnAliases().isEmpty());
        assertFalse(table3.columnAliases().isEmpty());
        assertFalse(table3.ordinality());
        assertTrue(table4.ordinality());
    }

    @Test
    @DisplayName("Accept visitor")
    void acceptVisitor() {
        var func = func("generate_series", arg(lit(1)), arg(lit(10)));
        var table = FunctionTable.of(func).as("t");

        var result = table.accept(new TestVisitor());
        assertEquals("FunctionTable", result);
    }

    @Test
    @DisplayName("Column aliases are copied")
    void columnAliasesAreCopied() {
        var func = func("generate_series", arg(lit(1)), arg(lit(10)));
        var columns = new java.util.ArrayList<>(List.of("a", "b"));
        var table = FunctionTable.of(func)
            .as("t")
            .columnAliases(columns);

        columns.add("c");
        assertEquals(2, table.columnAliases().size());
    }

    private static class TestVisitor extends io.sqm.core.walk.RecursiveNodeVisitor<String> {
        @Override
        protected String defaultResult() {
            return null;
        }

        @Override
        public String visitFunctionTable(FunctionTable t) {
            return "FunctionTable";
        }
    }
}
