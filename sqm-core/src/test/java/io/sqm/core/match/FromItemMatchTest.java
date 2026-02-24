package io.sqm.core.match;

import io.sqm.core.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FromItemMatch Tests")
class FromItemMatchTest {

    @Test
    @DisplayName("Match join")
    void matchJoin() {
        FromItem item = inner(tbl("users").as("u"))
            .on(col("u", "id").eq(col("o", "user_id")));

        var result = Match.<String>join(item)
            .join(j -> "Join")
            .tableRef(t -> "TableRef")
            .orElse("Unknown");

        assertEquals("Join", result);
    }

    @Test
    @DisplayName("Match table ref - base table")
    void matchTableRef() {
        FromItem item = tbl("users").as("u");

        var result = Match.<String>join(item)
            .join(j -> "Join")
            .tableRef(t -> "TableRef")
            .orElse("Unknown");

        assertEquals("TableRef", result);
    }

    @Test
    @DisplayName("Match table ref - query table")
    void matchQueryTableRef() {
        FromItem item = tbl(select(col("*")).from(tbl("users")).build()).as("sub");

        var result = Match.<String>join(item)
            .join(j -> "Join")
            .tableRef(t -> "TableRef")
            .orElse("Unknown");

        assertEquals("TableRef", result);
    }

    @Test
    @DisplayName("Match with no handler returns orElse")
    void matchNoHandlerReturnsOrElse() {
        FromItem item = tbl("users").as("u");

        var result = Match.<String>join(item)
            .join(j -> "Join")
            .orElse("Unknown");

        assertEquals("Unknown", result);
    }

    @Test
    @DisplayName("Match with otherwise function")
    void matchWithOtherwise() {
        FromItem item = cross(tbl("products").as("p"));

        var result = FromItemMatch.<String>match(item)
            .join(j -> "Join: " + j.getClass().getSimpleName())
            .otherwise(i -> "Other: " + i.getClass().getSimpleName());

        assertTrue(result.startsWith("Join:"));
    }

    @Test
    @DisplayName("Match returns first matching handler")
    void matchReturnsFirstMatch() {
        FromItem item = tbl("users").as("u");

        var result = FromItemMatch.<String>match(item)
            .tableRef(t -> "First")
            .tableRef(t -> "Second")
            .orElse("Unknown");

        assertEquals("First", result);
    }

    @Test
    @DisplayName("Match with orElseThrow")
    void matchWithOrElseThrow() {
        FromItem item = tbl("users").as("u");

        var result = FromItemMatch.<String>match(item)
            .tableRef(t -> "TableRef")
            .orElseThrow(() -> new RuntimeException("Not matched"));

        assertEquals("TableRef", result);
    }

    @Test
    @DisplayName("Match without handler throws with orElseThrow")
    void matchWithoutHandlerThrows() {
        FromItem item = tbl("users").as("u");

        assertThrows(RuntimeException.class, () ->
            FromItemMatch.<String>match(item)
                .join(j -> "Join")
                .orElseThrow(() -> new RuntimeException("Not matched"))
        );
    }

    @Test
    @DisplayName("Match with orElseGet")
    void matchWithOrElseGet() {
        FromItem item = tbl("users").as("u");

        var result = FromItemMatch.<String>match(item)
            .join(j -> "Join")
            .orElseGet(() -> "Generated default");

        assertEquals("Generated default", result);
    }

    @Test
    @DisplayName("Match with otherwiseEmpty returns empty")
    void matchOtherwiseEmptyReturnsEmpty() {
        FromItem item = tbl("users").as("u");

        var result = FromItemMatch.<String>match(item)
            .join(j -> "Join")
            .otherwiseEmpty();

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Match with otherwiseEmpty returns value")
    void matchOtherwiseEmptyReturnsValue() {
        FromItem item = tbl("users").as("u");

        var result = FromItemMatch.<String>match(item)
            .tableRef(t -> "TableRef")
            .otherwiseEmpty();

        assertTrue(result.isPresent());
        assertEquals("TableRef", result.get());
    }

    @Test
    @DisplayName("Match different join types")
    void matchDifferentJoinTypes() {
        FromItem innerJoin = inner(tbl("users").as("u")).on(col("u", "id").eq(lit(1)));
        FromItem crossJoin = cross(tbl("products").as("p"));
        FromItem usingJoin = inner(tbl("orders").as("o")).using("id");

        assertEquals("Join", FromItemMatch.<String>match(innerJoin).join(j -> "Join").orElse("Unknown"));
        assertEquals("Join", FromItemMatch.<String>match(crossJoin).join(j -> "Join").orElse("Unknown"));
        assertEquals("Join", FromItemMatch.<String>match(usingJoin).join(j -> "Join").orElse("Unknown"));
    }

    @Test
    @DisplayName("Match lateral")
    void matchLateral() {
        FromItem item = tbl("users").as("u").lateral();

        var result = FromItemMatch.<String>match(item)
            .join(j -> "Join")
            .tableRef(t -> "TableRef")
            .orElse("Unknown");

        assertEquals("TableRef", result);
    }

    @Test
    @DisplayName("Fluent chaining")
    void fluentChaining() {
        FromItem item = tbl("users").as("u");

        var result = item.matchFromItem()
            .join(j -> "Join")
            .tableRef(t -> ((Table) t).name().value())
            .orElse("Unknown");

        assertEquals("users", result);
    }
}
