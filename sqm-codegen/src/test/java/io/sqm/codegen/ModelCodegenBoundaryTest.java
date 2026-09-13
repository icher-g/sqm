package io.sqm.codegen;

import io.sqm.core.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class ModelCodegenBoundaryTest {
    @Test
    void rejectsModelQueryUntilCodegenStoryLands() {
        var query = select(col("sales")).model(model().dimension("year").measure("sales")
            .rule(cellTarget("sales").address(2026).build(),
                cellRef("sales").selector(2025).build()).build()).build();
        var error = assertThrows(IllegalStateException.class, () -> new SqmDslVisitor().emit(query));
        assertTrue(error.getMessage().contains("MODEL"));
    }

    @Test
    void rejectsModelGeneratorsWithAllReusedValueSourcesUntilCodegenStoryLands() {
        for (var generator : List.of(cellForValues("year", 2026, 2027),
            cellForValues("year", rows(row(2026), row(2027))),
            cellForValues("year", select(col("forecast_year")).from(tbl("forecasts")).build()))) {
            var query = select(col("sales")).model(model().dimension("year").measure("sales")
                .rule(cellTarget("sales", generator), lit(100)).build()).build();
            var error = assertThrows(IllegalStateException.class, () -> new SqmDslVisitor().emit(query));
            assertTrue(error.getMessage().contains("MODEL"));
        }
    }

    @Test
    void rejectsModelExpressionsEvenWithoutAContainingModelClause() {
        for (Expression expression : List.<Expression>of(
            cellRef("sales", 2026),
            modelAggregateExpr(func("SUM", col("sales")), List.of(cellAny())),
            currentDimension("year"),
            iterationNumber(),
            isAny(),
            isAny("product"),
            previousModelValueExpr(cellRef("sales", 2026)),
            presenceValue(cellRef("sales", 2026), lit(10), lit(0)),
            cellIsPresent(cellRef("sales", 2026)))) {
            var error = assertThrows(IllegalStateException.class,
                () -> new SqmDslVisitor().emit(select(expression).build()));
            assertTrue(error.getMessage().contains("MODEL"));
        }
    }
}
