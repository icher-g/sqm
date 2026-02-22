package io.sqm.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Lateral Tests")
class LateralTest {

    @Test
    @DisplayName("Create lateral with query table")
    void createWithQueryTable() {
        var subquery = select(col("id")).from(tbl("users")).build();
        var queryTable = tbl(subquery).as("sub");
        var lateral = Lateral.of(queryTable);

        assertNotNull(lateral);
        assertEquals(queryTable, lateral.inner());
    }

    @Test
    @DisplayName("Create lateral with function table")
    void createWithFunctionTable() {
        var func = func("unnest", arg(col("arr")));
        var funcTable = func.asTable().as("t");
        var lateral = Lateral.of(funcTable);

        assertNotNull(lateral);
        assertEquals(funcTable, lateral.inner());
    }

    @Test
    @DisplayName("Create lateral with base table")
    void createWithBaseTable() {
        var table = tbl("users").as("u");
        var lateral = Lateral.of(table);

        assertNotNull(lateral);
        assertEquals(table, lateral.inner());
    }

    @Test
    @DisplayName("lateral() method on TableRef")
    void lateralMethodOnTableRef() {
        var subquery = select(col("*")).from(tbl("orders")).build();
        var queryTable = tbl(subquery).as("o");
        var lateral = queryTable.lateral();

        assertNotNull(lateral);
        assertInstanceOf(Lateral.class, lateral);
        assertEquals(queryTable, lateral.inner());
    }

    @Test
    @DisplayName("Nested lateral wrapping")
    void nestedLateral() {
        var table = tbl("users").as("u");
        var lateral1 = Lateral.of(table);
        var lateral2 = Lateral.of(lateral1);

        assertEquals(lateral1, lateral2.inner());
        assertEquals(table, lateral1.inner());
    }

    @Test
    @DisplayName("Accept visitor")
    void acceptVisitor() {
        var table = tbl("users").as("u");
        var lateral = Lateral.of(table);

        var result = lateral.accept(new TestVisitor());
        assertEquals("Lateral", result);
    }

    @Test
    @DisplayName("Lateral with VALUES table")
    void lateralWithValuesTable() {
        var values = tbl(rows(row(1, "a"), row(2, "b"))).as("v").columnAliases("id", "name");
        var lateral = Lateral.of(values);

        assertNotNull(lateral);
        assertEquals(values, lateral.inner());
    }

    @Test
    @DisplayName("Null inner throws exception")
    void nullInnerThrowsException() {
        assertThrows(NullPointerException.class, () -> Lateral.of(null));
    }

    private static class TestVisitor extends io.sqm.core.walk.RecursiveNodeVisitor<String> {
        @Override
        protected String defaultResult() {
            return null;
        }

        @Override
        public String visitLateral(Lateral i) {
            return "Lateral";
        }
    }
}
