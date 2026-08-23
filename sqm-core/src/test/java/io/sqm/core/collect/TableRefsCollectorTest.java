package io.sqm.core.collect;

import io.sqm.core.FunctionTable;
import io.sqm.core.Lateral;
import io.sqm.core.QueryTable;
import io.sqm.core.RowExpr;
import io.sqm.core.RowListExpr;
import io.sqm.core.ValuesTable;
import io.sqm.core.VariableTable;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TableRefsCollectorTest {

    @Test
    void collectsVisitedTableRefsInOrder() {
        var sales = tbl("sales");
        var values = ValuesTable.of(RowListExpr.of(List.of(RowExpr.of(List.of(lit(1))))));
        var function = FunctionTable.of(func("generate_series", lit(1), lit(3)));
        var users = tbl("users");
        var queryTable = QueryTable.of(select(col("id")).from(users).build());
        var variable = VariableTable.of("audit_rows");
        var facts = tbl("facts");
        var pivoted = pivot(
            facts,
            List.of(pivotMeasure(func("sum", col("amount")))),
            col("quarter"),
            pivotValue(lit("Q1"), "q1")
        );
        var wideSales = tbl("wide_sales");
        var unpivoted = unpivot(
            wideSales,
            "amount",
            "quarter",
            unpivotInput("q1", lit("Q1"))
        );
        var lateral = Lateral.of(values);

        var collector = new TableRefsCollector();
        collector.visitTable(sales);
        collector.visitValuesTable(values);
        collector.visitFunctionTable(function);
        collector.visitQueryTable(queryTable);
        collector.visitVariableTable(variable);
        collector.visitPivotTable(pivoted);
        collector.visitUnpivotTable(unpivoted);
        collector.visitLateral(lateral);

        assertEquals(
            List.of(
                sales,
                values,
                function,
                queryTable,
                users,
                variable,
                pivoted,
                facts,
                unpivoted,
                wideSales,
                lateral,
                values
            ),
            collector.getTableRefs()
        );
    }

    @Test
    void collectsPatternRecognitionRelationAndItsSource() {
        var table = matchRecognize(tbl("events"))
            .pattern(patternVar("A"))
            .define("A", patternColumn("A", "amount").gt(0))
            .build();
        var collector = new TableRefsCollector();

        table.accept(collector);

        assertEquals(List.of(table, table.source()), collector.getTableRefs());
    }
}
