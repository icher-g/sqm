package io.sqm.core.walk;

import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PivotRecursiveNodeVisitorTest {

    @Test
    void visitsPivotTableChildren() {
        var visitor = new RecordingVisitor();
        var pivot = pivot(
            tbl("sales"),
            List.of(pivotMeasure(func("sum", col("amount")))),
            col("quarter"),
            List.of(pivotValue(lit("Q1")))
        );

        pivot.accept(visitor);

        assertTrue(visitor.seen().contains("PivotTable"));
        assertTrue(visitor.seen().contains("Table"));
        assertTrue(visitor.seen().contains("PivotMeasure"));
        assertTrue(visitor.seen().contains("FunctionExpr"));
        assertTrue(visitor.seen().contains("ColumnExpr"));
        assertTrue(visitor.seen().contains("PivotValue"));
        assertTrue(visitor.seen().contains("LiteralExpr"));
    }

    @Test
    void visitsUnpivotTableChildren() {
        var visitor = new RecordingVisitor();
        var unpivot = unpivot(
            tbl("sales"),
            "amount",
            "quarter",
            List.of(unpivotInput("q1", lit("Q1")))
        );

        unpivot.accept(visitor);

        assertTrue(visitor.seen().contains("UnpivotTable"));
        assertTrue(visitor.seen().contains("Table"));
        assertTrue(visitor.seen().contains("UnpivotInput"));
        assertTrue(visitor.seen().contains("LiteralExpr"));
    }
}
