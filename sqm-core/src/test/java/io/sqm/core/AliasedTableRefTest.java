package io.sqm.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AliasedTableRef Tests")
class AliasedTableRefTest {

    @Test
    @DisplayName("QueryTable implements AliasedTableRef")
    void queryTableImplementsAliasedTableRef() {
        var query = select(col("*")).from(tbl("users")).build();
        AliasedTableRef aliased = tbl(query).as("sub").columnAliases("id", "name");

        assertEquals("sub", aliased.alias());
        assertEquals(2, aliased.columnAliases().size());
        assertTrue(aliased.columnAliases().contains("id"));
        assertTrue(aliased.columnAliases().contains("name"));
    }

    @Test
    @DisplayName("ValuesTable implements AliasedTableRef")
    void valuesTableImplementsAliasedTableRef() {
        AliasedTableRef aliased = tbl(rows(row(1, "a"), row(2, "b")))
            .as("v")
            .columnAliases("id", "name");

        assertEquals("v", aliased.alias());
        assertEquals(2, aliased.columnAliases().size());
        assertEquals("id", aliased.columnAliases().get(0));
        assertEquals("name", aliased.columnAliases().get(1));
    }

    @Test
    @DisplayName("FunctionTable implements AliasedTableRef")
    void functionTableImplementsAliasedTableRef() {
        var func = func("generate_series", arg(lit(1)), arg(lit(10)));
        AliasedTableRef aliased = func.asTable().as("series").columnAliases("num");

        assertEquals("series", aliased.alias());
        assertEquals(1, aliased.columnAliases().size());
        assertEquals("num", aliased.columnAliases().getFirst());
    }

    @Test
    @DisplayName("AliasedTableRef with null alias")
    void aliasedTableRefWithNullAlias() {
        var query = select(col("*")).from(tbl("users")).build();
        AliasedTableRef aliased = tbl(query);

        assertNull(aliased.alias());
        assertTrue(aliased.columnAliases().isEmpty());
    }

    @Test
    @DisplayName("AliasedTableRef with empty column aliases")
    void aliasedTableRefWithEmptyColumnAliases() {
        var func = func("unnest", arg(array(lit(1), lit(2))));
        AliasedTableRef aliased = func.asTable().as("t").columnAliases(List.of());

        assertEquals("t", aliased.alias());
        assertTrue(aliased.columnAliases().isEmpty());
    }

    @Test
    @DisplayName("Column aliases are immutable")
    void columnAliasesAreImmutable() {
        var func = func("json_each", arg(col("data")));
        AliasedTableRef aliased = func.asTable().as("t").columnAliases("key", "value");

        assertThrows(UnsupportedOperationException.class, () ->
            aliased.columnAliases().add("extra")
        );
    }

    @Test
    @DisplayName("Multiple AliasedTableRef types in query")
    void multipleAliasedTableRefTypesInQuery() {
        var query = select(
            col("sub", "id"),
            col("v", "name"),
            col("s", "num")
        )
            .from(tbl(select(col("id")).from(tbl("users")).build()).as("sub").columnAliases("id"))
            .join(inner(tbl(rows(row(1, "a"))).as("v").columnAliases("id", "name"))
                .on(col("sub", "id").eq(col("v", "id"))))
            .join(inner(func("generate_series", arg(lit(1)), arg(lit(10)))
                .asTable().as("s").columnAliases("num"))
                .on(col("s", "num").eq(col("v", "id"))))
            .build();

        assertNotNull(query);
        assertEquals(2, query.joins().size());
    }
}
