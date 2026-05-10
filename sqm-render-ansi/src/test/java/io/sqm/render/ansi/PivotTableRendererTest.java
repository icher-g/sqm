package io.sqm.render.ansi;

import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PivotTableRendererTest {
    private final RenderContext ctx = RenderContext.of(new AnsiDialect());

    @Test
    void rejectsPivotTables() {
        var pivoted = pivot(
            tbl("sales"),
            List.of(pivotMeasure(func("sum", col("amount")))),
            col("quarter"),
            List.of(pivotValue(lit("Q1"))));

        assertThrows(UnsupportedDialectFeatureException.class, () -> ctx.render(pivoted));
    }

    @Test
    void rejectsUnpivotTables() {
        var unpivoted = unpivot(
            tbl("sales"),
            "amount",
            "quarter",
            List.of(unpivotInput("q1", lit("Q1"))));

        assertThrows(UnsupportedDialectFeatureException.class, () -> ctx.render(unpivoted));
    }
}
