package io.sqm.core.transform;

import io.sqm.core.*;
import org.junit.jupiter.api.Test;
import java.util.List;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class ModelRewriteScopeTest {
    @Test void modelDeclarationsSeeJoinedInputTablesWhileOutputKeepsItsOwnNamespace() {
        var query = select(col("sales")).from(tbl("data").as("d"))
            .join(inner(tbl("lookup").as("l")).on(col("d", "k").eq(col("l", "k"))))
            .join(inner(tbl("years").as("y")).using("k"))
            .join(cross(tbl("rates").as("r")))
            .join(natural(tbl("regions").as("n")))
            .model(model().dimension("k").measure("amount", "sales").rule(cellTarget("sales", 1), lit(1)).build()).build();
        var transformer = ColumnQualificationTransformer.of((name, tables) -> {
            assertEquals(List.of("d", "l", "y", "r", "n"),
                tables.stream().map(table -> table.qualifier().value()).toList());
            return ColumnQualification.qualified(tables.getLast().qualifier());
        });
        var result = (SelectQuery) transformer.apply(query);
        assertNull(((ColumnExpr) ((ExprSelectItem) result.items().getFirst()).expr()).tableAlias());
        assertEquals("n", ((ColumnExpr) result.model().main().measures().getFirst().expression()).tableAlias().value());
    }

    @Test void normalizationCoversEveryModelIdentifierAndIsIdempotent() {
        var reference = referenceModel("GROWTH", select(col("P"), col("RATE")).from(tbl("RATES")).build())
            .dimension("P").measure("RATE").build();
        var main = mainModel().name("FORECAST").dimension("P").dimension("Y").measure("AMOUNT", "SALES")
            .rules(modelRules().rule(cellTarget("SALES", cellValue("Bike"), cellForValues("Y", 1, 2)),
                cellRef(id("GROWTH"), id("RATE"), cellValue("Bike")).add(currentDimension("Y")))
                .rule(cellTarget("SALES", cellValue("Bike"), cellForRange("Y", 1, 2, RangeDirection.INCREMENT, 1)),
                    cellRef("SALES", "Bike", 1)).build()).build();
        var query = select(col("SALES")).from(tbl("DATA")).model(model().reference(reference).main(main).build()).build();
        var normalizer = new IdentifierNormalizationTransformer();
        var result = (SelectQuery) normalizer.transform(query);
        var model = result.model();
        assertEquals("forecast", model.main().name().value());
        assertEquals("growth", model.references().getFirst().name().value());
        assertEquals("sales", model.main().measures().getFirst().name().value());
        var rule = model.main().rules().rules().getFirst();
        assertEquals("sales", rule.target().measure().value());
        assertEquals("y", ((CellFor.Values) rule.target().addresses().get(1)).dimensions().getFirst().value());
        var expression = (BinaryArithmeticExpr) rule.value();
        assertEquals("growth", ((CellRefExpr) expression.lhs()).model().value());
        assertEquals("rate", ((CellRefExpr) expression.lhs()).measure().value());
        assertEquals("y", ((CurrentDimensionExpr) expression.rhs()).dimension().value());
        assertEquals("y", ((CellFor.Range) model.main().rules().rules().get(1).target().addresses().get(1)).dimension().value());
        assertSame(result, normalizer.transform(result));
        var quotedName = id("SALES", QuoteStyle.DOUBLE_QUOTE);
        var quoted = select(col(quotedName)).model(model().dimension("y")
            .measure(modelColumn(lit(0), quotedName)).rules(modelRules()
                .rule(cellTarget(quotedName).address(1).build(), lit(1)).build()).build()).build();
        assertSame(quoted, normalizer.transform(quoted));
        var bare = currentDimension();
        assertSame(bare, normalizer.transform(bare));
    }

    @Test void qualificationUsesInputRowsForDeclarationsAndLeavesModelColumnsUnqualified() {
        var generator = select(col("year_key")).from(tbl("years").as("ys")).build();
        var reference = referenceModel("growth", select(col("p"), col("rate")).from(tbl("rates").as("r")).build())
            .dimension("p").measure("rate").build();
        var query = select(col("sales")).from(tbl("data").as("s")).where(col("amount").gt(0))
            .model(model().reference(reference).dimension("p").dimension("y").measure("amount", "sales")
                .rules(modelRules().rule(cellTarget("sales", cellCondition(col("p").eq("Bike")), cellForValues("y", generator)),
                    cellRef("sales", "Bike", currentDimension("y"))).build()).build())
            .orderBy(col("sales")).build();
        var transformer = ColumnQualificationTransformer.of((name, tables) -> tables.isEmpty()
            ? ColumnQualification.unresolved() : ColumnQualification.qualified(tables.getFirst().qualifier()));
        var result = (SelectQuery) transformer.apply(query);
        assertNull(((ColumnExpr) ((ExprSelectItem) result.items().getFirst()).expr()).tableAlias());
        assertNull(((ColumnExpr) result.orderBy().items().getFirst().expr()).tableAlias());
        assertEquals("s", ((ColumnExpr) result.model().main().measures().getFirst().expression()).tableAlias().value());
        var rule = result.model().main().rules().rules().getFirst();
        assertSame(query.model().main().rules().rules().getFirst().target().addresses().getFirst(), rule.target().addresses().getFirst());
        var forQuery = (SelectQuery) ((QueryExpr) ((CellFor.Values) rule.target().addresses().get(1)).values()).subquery();
        assertEquals("ys", ((ColumnExpr) ((ExprSelectItem) forQuery.items().getFirst()).expr()).tableAlias().value());
        var ref = result.model().references().getFirst();
        assertNull(((ColumnExpr) ref.dimensions().getFirst().expression()).tableAlias());
        assertEquals("r", ((ColumnExpr) ((ExprSelectItem) ((SelectQuery) ref.query()).items().getFirst()).expr()).tableAlias().value());
        assertSame(result, transformer.apply(result));
        assertSame(query, ColumnQualificationTransformer.of((name, tables) -> ColumnQualification.unresolved()).apply(query));
    }
}
