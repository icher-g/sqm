package io.sqm.render.ansi;

import io.sqm.core.UnpivotTable;
import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PivotTableRendererTest {
    private final RenderContext ctx = RenderContext.of(new AnsiDialect());
    private final RenderContext enabled = RenderContext.of(new PivotAnsiDialect());

    @Test
    void rejectsPivotTables() {
        var pivoted = pivot(
            tbl("sales"),
            List.of(pivotMeasure(func("sum", col("amount")))),
            col("quarter"),
            pivotValue(lit("Q1")));

        assertThrows(UnsupportedDialectFeatureException.class, () -> ctx.render(pivoted));
    }

    @Test
    void rejectsUnpivotTables() {
        var unpivoted = unpivot(
            tbl("sales"),
            "amount",
            "quarter",
            unpivotInput("q1", lit("Q1")));

        assertThrows(UnsupportedDialectFeatureException.class, () -> ctx.render(unpivoted));
    }

    @Test
    void rendersPivotTableWhenFeatureIsEnabled() {
        var query = select(star())
            .from(pivot(
                tbl("sales"),
                List.of(
                    pivotMeasure(func("sum", col("amount")), "total"),
                    pivotMeasure(func("max", col("discount")))),
                col("quarter"),
                pivotValue(lit("Q1"), "q1"), pivotValue(lit("Q2")))
                .as("p"))
            .build();

        assertEquals(
            "SELECT * FROM sales PIVOT ( sum(amount) AS total, max(discount) FOR quarter IN ( 'Q1' AS q1, 'Q2' ) ) AS p",
            normalize(enabled.render(query).sql())
        );
    }

    @Test
    void rendersUnpivotTableShapesWhenFeatureIsEnabled() {
        var includeNulls = UnpivotTable.of(
            tbl("sales"),
            List.of(id("amount"), id("quantity")),
            id("quarter"),
            List.of(
                unpivotInput(List.of(id("q1_amount"), id("q1_quantity")), lit("Q1")),
                unpivotInput("q2_amount", lit("Q2"))),
            UnpivotTable.NullTreatment.INCLUDE_NULLS
        ).as("u");
        var excludeNulls = UnpivotTable.of(
            tbl("sales"),
            List.of(id("amount")),
            id("quarter"),
            List.of(unpivotInput("q1", lit("Q1"))),
            UnpivotTable.NullTreatment.EXCLUDE_NULLS
        );

        assertEquals(
            "sales UNPIVOT INCLUDE NULLS ( (amount, quantity) FOR quarter IN ( (q1_amount, q1_quantity) AS 'Q1', q2_amount AS 'Q2' ) ) AS u",
            normalize(enabled.render(includeNulls).sql())
        );
        assertEquals(
            "sales UNPIVOT EXCLUDE NULLS ( amount FOR quarter IN ( q1 AS 'Q1' ) )",
            normalize(enabled.render(excludeNulls).sql())
        );
    }

    @Test
    void rendersUnpivotTableWithDialectDefaultNullTreatment() {
        var unpivoted = unpivot(
            tbl("sales"),
            "amount",
            "quarter",
            unpivotInput("q1", lit("Q1"))
        );

        assertEquals(
            "sales UNPIVOT ( amount FOR quarter IN ( q1 AS 'Q1' ) )",
            normalize(enabled.render(unpivoted).sql())
        );
    }

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }

    private static final class PivotAnsiDialect extends AnsiDialect {
        private final DialectCapabilities capabilities = io.sqm.core.dialect.VersionedDialectCapabilities
            .builder(SqlDialectVersion.of(2016))
            .supports(SqlFeature.PIVOT_TABLE)
            .supports(SqlFeature.UNPIVOT_TABLE)
            .build();

        @Override
        public DialectCapabilities capabilities() {
            return capabilities;
        }
    }
}
