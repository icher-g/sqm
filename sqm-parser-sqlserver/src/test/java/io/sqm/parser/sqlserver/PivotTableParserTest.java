package io.sqm.parser.sqlserver;

import io.sqm.core.PivotTable;
import io.sqm.core.Query;
import io.sqm.core.SelectQuery;
import io.sqm.core.UnpivotTable;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.sqlserver.spi.SqlServerSpecs;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class PivotTableParserTest {
    private final ParseContext ctx = ParseContext.of(new SqlServerSpecs());

    @Test
    void parsesSqlServerPivotTable() {
        var result = ctx.parse(Query.class, """
            SELECT *
            FROM sales
            PIVOT (
                sum(amount) FOR quarter IN ([Q1], [Q2])
            ) p
            """);

        assertTrue(result.ok(), result.errorMessage());
        var query = assertInstanceOf(SelectQuery.class, result.value());
        var pivot = assertInstanceOf(PivotTable.class, query.from());
        assertEquals("p", pivot.alias().value());
        assertEquals(2, pivot.values().size());
        assertEquals("Q1", pivot.values().getFirst().value().matchExpression().column(c -> c.name().value()).orElse(null));
    }

    @Test
    void parsesSqlServerUnpivotTable() {
        var result = ctx.parse(Query.class, """
            SELECT *
            FROM sales
            UNPIVOT (
                amount FOR quarter IN ([Q1], [Q2])
            ) u
            """);

        assertTrue(result.ok(), result.errorMessage());
        var query = assertInstanceOf(SelectQuery.class, result.value());
        var unpivot = assertInstanceOf(UnpivotTable.class, query.from());
        assertEquals("u", unpivot.alias().value());
        assertEquals(2, unpivot.inputs().size());
        assertEquals("Q2", unpivot.inputs().get(1).sourceColumns().getFirst().value());
    }

    @Test
    void rejectsOracleOnlyPivotAndUnpivotOptions() {
        var pivotAlias = ctx.parse(Query.class, "SELECT * FROM sales PIVOT (sum(amount) AS total FOR quarter IN ([Q1])) p");
        var pivotValueAlias = ctx.parse(Query.class, "SELECT * FROM sales PIVOT (sum(amount) FOR quarter IN ([Q1] AS q1)) p");
        var unpivotNulls = ctx.parse(Query.class, "SELECT * FROM sales UNPIVOT INCLUDE NULLS (amount FOR quarter IN ([Q1])) u");
        var unpivotLabel = ctx.parse(Query.class, "SELECT * FROM sales UNPIVOT (amount FOR quarter IN ([Q1] AS 'Q1')) u");

        assertTrue(pivotAlias.isError());
        assertTrue(Objects.requireNonNull(pivotAlias.errorMessage()).contains("measure aliases"));
        assertTrue(pivotValueAlias.isError());
        assertTrue(Objects.requireNonNull(pivotValueAlias.errorMessage()).contains("value aliases"));
        assertTrue(unpivotNulls.isError());
        assertTrue(Objects.requireNonNull(unpivotNulls.errorMessage()).contains("null treatment"));
        assertTrue(unpivotLabel.isError());
        assertTrue(Objects.requireNonNull(unpivotLabel.errorMessage()).contains("input labels"));
    }
}
