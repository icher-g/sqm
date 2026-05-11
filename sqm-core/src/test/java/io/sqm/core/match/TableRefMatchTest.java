package io.sqm.core.match;

import io.sqm.core.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TableRefMatch with FunctionTable and Lateral Tests")
class TableRefMatchTest {

    @Test
    @DisplayName("Match function table")
    void matchFunctionTable() {
        var func = func("generate_series", lit(1), lit(10));
        TableRef table = func.asTable().as("t");

        var result = table.matchTableRef()
            .table(t -> "Table")
            .query(q -> "Query")
            .values(v -> "Values")
            .function(f -> "Function")
            .orElse("Unknown");

        assertEquals("Function", result);
    }

    @Test
    @DisplayName("Match lateral")
    void matchLateral() {
        var subquery = select(col("*")).from(tbl("users")).build();
        TableRef table = tbl(subquery).as("sub").lateral();

        var result = table.matchTableRef()
            .table(t -> "Table")
            .query(q -> "Query")
            .lateral(l -> "Lateral")
            .orElse("Unknown");

        assertEquals("Lateral", result);
    }

    @Test
    @DisplayName("Match base table")
    void matchBaseTable() {
        TableRef table = tbl("users").as("u");

        var result = table.matchTableRef()
            .table(t -> "Table")
            .query(q -> "Query")
            .values(v -> "Values")
            .function(f -> "Function")
            .lateral(l -> "Lateral")
            .orElse("Unknown");

        assertEquals("Table", result);
    }

    @Test
    @DisplayName("Match query table")
    void matchQueryTable() {
        var subquery = select(col("*")).from(tbl("users")).build();
        TableRef table = tbl(subquery).as("sub");

        var result = table.matchTableRef()
            .table(t -> "Table")
            .query(q -> "Query")
            .values(v -> "Values")
            .function(f -> "Function")
            .lateral(l -> "Lateral")
            .orElse("Unknown");

        assertEquals("Query", result);
    }

    @Test
    @DisplayName("Match values table")
    void matchValuesTable() {
        TableRef table = tbl(rows(row(1, "a"), row(2, "b"))).as("v");

        var result = table.matchTableRef()
            .table(t -> "Table")
            .query(q -> "Query")
            .values(v -> "Values")
            .function(f -> "Function")
            .lateral(l -> "Lateral")
            .orElse("Unknown");

        assertEquals("Values", result);
    }

    @Test
    @DisplayName("Extract function from function table")
    void extractFunctionFromFunctionTable() {
        var func = func("unnest", array(lit(1), lit(2), lit(3)));
        TableRef table = func.asTable().as("t");

        var result = table.matchTableRef()
            .function(f -> String.join(".", f.function().name().values()))
            .orElse("Unknown");

        assertEquals("unnest", result);
    }

    @Test
    @DisplayName("Extract inner from lateral")
    void extractInnerFromLateral() {
        var subquery = select(col("*")).from(tbl("users")).build();
        var queryTable = tbl(subquery).as("sub");
        TableRef lateral = queryTable.lateral();

        var result = lateral.matchTableRef()
            .lateral(l -> l.inner().matchTableRef()
                .query(q -> "QueryTable")
                .orElse("Unknown"))
            .orElse("Unknown");

        assertEquals("QueryTable", result);
    }

    @Test
    @DisplayName("Match with no handler returns orElse")
    void matchNoHandlerReturnsOrElse() {
        TableRef table = tbl("users").as("u");

        var result = table.matchTableRef()
            .query(q -> "Query")
            .values(v -> "Values")
            .function(f -> "Function")
            .lateral(l -> "Lateral")
            .orElse("Unknown");

        assertEquals("Unknown", result);
    }

    @Test
    @DisplayName("Match returns first matching handler")
    void matchReturnsFirstMatch() {
        TableRef table = tbl("users").as("u");

        var result = table.matchTableRef()
            .table(t -> "First")
            .table(t -> "Second")
            .orElse("Unknown");

        assertEquals("First", result);
    }

    @Test
    @DisplayName("Match with complex extraction")
    void matchWithComplexExtraction() {
        var func = func("json_each", col("data"));
        TableRef table = func.asTable().as("t").columnAliases("key", "value");

        var result = table.matchTableRef()
            .function(f -> {
                var alias = f.alias();
                var colCount = f.columnAliases().size();
                return alias.value() + ":" + colCount;
            })
            .orElse("Unknown");

        assertEquals("t:2", result);
    }

    @Test
    @DisplayName("Match lateral wrapping function table")
    void matchLateralWrappingFunctionTable() {
        var func = func("unnest", col("arr"));
        TableRef table = func.asTable().as("t").lateral();

        var result = table.matchTableRef()
            .lateral(l -> l.inner().matchTableRef()
                .function(f -> "Function: " + String.join(".", f.function().name().values()))
                .orElse("Unknown"))
            .orElse("Not Lateral");

        assertEquals("Function: unnest", result);
    }

    @Test
    @DisplayName("Match with otherwiseEmpty")
    void matchWithOtherwiseEmpty() {
        var func = func("generate_series", lit(1), lit(10));
        TableRef table = func.asTable();

        var result = table.matchTableRef()
            .table(t -> "Table")
            .query(q -> "Query")
            .otherwiseEmpty();

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Match function table with otherwiseEmpty returns value")
    void matchFunctionTableWithOtherwiseEmptyReturnsValue() {
        var func = func("generate_series", lit(1), lit(10));
        TableRef table = func.asTable();

        var result = table.matchTableRef()
            .function(f -> "Function")
            .otherwiseEmpty();

        assertTrue(result.isPresent());
        assertEquals("Function", result.get());
    }

    @Test
    @DisplayName("Match variable table reference")
    void matchVariableTable() {
        TableRef table = tableVar("audit_rows");

        var result = table.matchTableRef()
            .variableTable(v -> v.name().value())
            .orElse("Unknown");

        assertEquals("audit_rows", result);
    }

    @Test
    @DisplayName("Match pivot table reference")
    void matchPivotTable() {
        TableRef table = pivot(
            tbl("sales"),
            java.util.List.of(pivotMeasure(func("sum", col("amount")))),
            col("quarter"),
            java.util.List.of(pivotValue(lit("Q1"))));

        var result = table.matchTableRef()
            .pivot(p -> p.forExpression().matchExpression().column(c -> c.name().value()).orElse("Unknown"))
            .orElse("Unknown");

        assertEquals("quarter", result);
    }

    @Test
    @DisplayName("Match unpivot table reference")
    void matchUnpivotTable() {
        TableRef table = unpivot(
            tbl("sales"),
            "amount",
            "quarter",
            java.util.List.of(unpivotInput("q1", lit("Q1"))));

        var result = table.matchTableRef()
            .unpivot(u -> u.nameColumn().value())
            .orElse("Unknown");

        assertEquals("quarter", result);
    }
}
