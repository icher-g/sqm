package io.sqm.render.sqlserver;

import io.sqm.render.sqlserver.spi.SqlServerDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PivotTableRendererTest {
    private final RenderContext ctx = RenderContext.of(new SqlServerDialect());

    @Test
    void rendersPivotTable() {
        var pivoted = pivot(
            tbl("sales"),
            List.of(pivotMeasure(func("sum", col("amount")))),
            col("quarter"),
            List.of(
                pivotValue(col(id("Q1", io.sqm.core.QuoteStyle.BRACKETS))),
                pivotValue(col(id("Q2", io.sqm.core.QuoteStyle.BRACKETS)))))
            .as("p");
        var query = select(star())
            .from(pivoted)
            .build();

        assertEquals(
            "SELECT * FROM sales PIVOT (sum(amount) FOR quarter IN ([Q1], [Q2])) AS p",
            normalize(ctx.render(query).sql())
        );
    }

    @Test
    void rendersUnpivotTable() {
        var query = select(star())
            .from(unpivot(
                tbl("sales"),
                "amount",
                "quarter",
                List.of(unpivotInput("q1", lit("q1")), unpivotInput("q2", lit("q2"))))
                .as("u"))
            .build();

        assertEquals(
            "SELECT * FROM sales UNPIVOT (amount FOR quarter IN (q1, q2)) AS u",
            normalize(ctx.render(query).sql())
        );
    }

    @Test
    void rejectsOracleOnlyAliasesAndNullTreatment() {
        var pivotWithMeasureAlias = pivot(
            tbl("sales"),
            List.of(pivotMeasure(func("sum", col("amount")), "total")),
            col("quarter"),
            List.of(pivotValue(col(id("Q1", io.sqm.core.QuoteStyle.BRACKETS)))));
        var pivotWithValueAlias = pivot(
            tbl("sales"),
            List.of(pivotMeasure(func("sum", col("amount")))),
            col("quarter"),
            List.of(pivotValue(col(id("Q1", io.sqm.core.QuoteStyle.BRACKETS)), "q1")));
        var unpivotWithNullTreatment = io.sqm.core.UnpivotTable.of(
            tbl("sales"),
            List.of(id("amount")),
            id("quarter"),
            List.of(unpivotInput("q1", lit("q1"))),
            io.sqm.core.UnpivotTable.NullTreatment.INCLUDE_NULLS);

        assertThrows(io.sqm.core.dialect.UnsupportedDialectFeatureException.class, () -> ctx.render(pivotWithMeasureAlias));
        assertThrows(io.sqm.core.dialect.UnsupportedDialectFeatureException.class, () -> ctx.render(pivotWithValueAlias));
        assertThrows(io.sqm.core.dialect.UnsupportedDialectFeatureException.class, () -> ctx.render(unpivotWithNullTreatment));
    }

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
