package io.sqm.render.oracle;

import io.sqm.render.oracle.spi.OracleDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PivotTableRendererTest {
    private final RenderContext ctx = RenderContext.of(new OracleDialect());

    @Test
    void rendersPivotTable() {
        var query = select(star())
            .from(pivot(
                tbl("sales"),
                List.of(pivotMeasure(func("sum", col("amount")), "total")),
                col("quarter"),
                pivotValue(lit("Q1"), "q1"), pivotValue(lit("Q2"), "q2")))
            .build();

        assertEquals(
            "SELECT * FROM sales PIVOT ( sum(amount) AS total FOR quarter IN ( 'Q1' AS q1, 'Q2' AS q2 ) )",
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
                unpivotInput("q1", lit("Q1")), unpivotInput("q2", lit("Q2")))
                .as("u"))
            .build();

        assertEquals(
            "SELECT * FROM sales UNPIVOT ( amount FOR quarter IN ( q1 AS 'Q1', q2 AS 'Q2' ) ) AS u",
            normalize(ctx.render(query).sql())
        );
    }

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
