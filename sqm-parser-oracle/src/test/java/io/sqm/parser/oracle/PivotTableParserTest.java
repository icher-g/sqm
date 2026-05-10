package io.sqm.parser.oracle;

import io.sqm.core.PivotTable;
import io.sqm.core.QuoteStyle;
import io.sqm.core.Query;
import io.sqm.core.SelectQuery;
import io.sqm.core.UnpivotTable;
import io.sqm.parser.ansi.AnsiSpecs;
import io.sqm.parser.oracle.spi.OracleSpecs;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class PivotTableParserTest {
    private final ParseContext ctx = ParseContext.of(new OracleSpecs());

    @Test
    void parsesOraclePivotTable() {
        var result = ctx.parse(Query.class, """
            SELECT *
            FROM sales
            PIVOT (
                sum(amount) AS "Total"
                FOR quarter IN ('Q1' AS q1, 'Q2' AS q2)
            )
            """);

        assertTrue(result.ok(), result.errorMessage());
        var query = assertInstanceOf(SelectQuery.class, result.value());
        var pivot = assertInstanceOf(PivotTable.class, query.from());
        assertEquals(1, pivot.measures().size());
        assertEquals("Total", pivot.measures().getFirst().alias().value());
        assertEquals(QuoteStyle.DOUBLE_QUOTE, pivot.measures().getFirst().alias().quoteStyle());
        assertEquals("quarter", pivot.forExpression().matchExpression().column(c -> c.name().value()).orElse(null));
        assertEquals("q1", pivot.values().getFirst().alias().value());
    }

    @Test
    void parsesOracleUnpivotTable() {
        var result = ctx.parse(Query.class, """
            SELECT *
            FROM sales
            UNPIVOT INCLUDE NULLS (
                amount FOR quarter IN (q1 AS 'Q1', q2 AS 'Q2')
            ) u
            """);

        assertTrue(result.ok(), result.errorMessage());
        var query = assertInstanceOf(SelectQuery.class, result.value());
        var unpivot = assertInstanceOf(UnpivotTable.class, query.from());
        assertEquals(UnpivotTable.NullTreatment.INCLUDE_NULLS, unpivot.nullTreatment());
        assertEquals("amount", unpivot.valueColumns().getFirst().value());
        assertEquals("quarter", unpivot.nameColumn().value());
        assertEquals("u", unpivot.alias().value());
        assertEquals(2, unpivot.inputs().size());
    }

    @Test
    void rejectsPivotXmlAndUnsupportedDialects() {
        var pivotXml = ctx.parse(Query.class, "SELECT * FROM sales PIVOT XML (sum(amount) FOR quarter IN ('Q1'))");
        var ansi = ParseContext.of(new AnsiSpecs())
            .parse(Query.class, "SELECT * FROM sales PIVOT (sum(amount) FOR quarter IN ('Q1'))");

        assertTrue(pivotXml.isError());
        assertTrue(Objects.requireNonNull(pivotXml.errorMessage()).contains("PIVOT XML"));
        assertTrue(ansi.isError());
        assertTrue(Objects.requireNonNull(ansi.errorMessage()).contains("PIVOT is not supported"));
    }

    @Test
    void rejectsNonFunctionPivotMeasures() {
        var result = ctx.parse(Query.class, "SELECT * FROM sales PIVOT (amount + 1 FOR quarter IN ('Q1'))");

        assertTrue(result.isError());
    }
}
