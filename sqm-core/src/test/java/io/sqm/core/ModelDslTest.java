package io.sqm.core;

import org.junit.jupiter.api.Test;
import java.util.List;
import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class ModelDslTest {
    @Test
    void fullStateHelpersRetainEveryField() {
        var modelColumnNode = modelColumn(lit(10), "sales");
        assertEquals(modelColumnNode, modelColumn(modelColumnNode.expression(), modelColumnNode.name()));
        var cellRefExprNode = cellRef("sales", 2026);
        assertEquals(cellRefExprNode, cellRefExpr(cellRefExprNode.model(), cellRefExprNode.measure(), cellRefExprNode.selectors()));
        var cellTargetNode = cellTarget("sales", 2026);
        assertEquals(cellTargetNode, cellTarget(cellTargetNode.measure(), cellTargetNode.addresses()));
        var modelAggregateExprNode = modelAggregateExpr(func("SUM", col("sales")), List.of(cellAny()));
        assertEquals(modelAggregateExprNode, modelAggregateExpr(modelAggregateExprNode.aggregate(), modelAggregateExprNode.selectors()));
        var currentDimensionExprNode = currentDimension("year");
        assertEquals(currentDimensionExprNode, currentDimensionExpr(currentDimensionExprNode.dimension()));
        var iterationNumberExprNode = iterationNumber();
        assertEquals(iterationNumberExprNode, iterationNumberExpr());
        var previousModelValueExprNode = previousModelValueExpr(cellRef("sales", 2026));
        assertEquals(previousModelValueExprNode, previousModelValueExpr(previousModelValueExprNode.cell()));
        var presenceValueExprNode = presenceValue(cellRef("sales", 2026), lit(10), lit(0));
        assertEquals(presenceValueExprNode, presenceValueExpr(presenceValueExprNode.mode(), presenceValueExprNode.cell(), presenceValueExprNode.whenPresent(), presenceValueExprNode.whenAbsent()));
        var cellPresentPredicateNode = cellIsPresent(cellRef("sales", 2026));
        assertEquals(cellPresentPredicateNode, cellPresentPredicate(cellPresentPredicateNode.cell()));
        var iterationSpecNode = iterationSpec(lit(10), cellRef("sales", 2026).gt(lit(0)));
        assertEquals(iterationSpecNode, iterationSpec(iterationSpecNode.limit(), iterationSpecNode.until()));
        var modelRuleNode = modelRule(RuleMode.UPDATE, cellTarget("sales", 2026), OrderBy.of(List.of(col("year").asc())), lit(20));
        assertEquals(modelRuleNode, modelRule(modelRuleNode.mode(), modelRuleNode.target(), modelRuleNode.orderBy(), modelRuleNode.value()));
        var modelRulesNode = modelRules().rule(cellTarget("sales", 2026), lit(20)).iteration(iterationSpec(lit(2), null)).build();
        assertEquals(modelRulesNode, modelRules(modelRulesNode.defaultMode(), modelRulesNode.order(), modelRulesNode.iteration(), modelRulesNode.rules()));
        var mainModelNode = mainModel().dimension("year").measure(lit(10), "sales").rule(cellTarget("sales", 2026), lit(20)).build();
        assertEquals(mainModelNode, mainModel(mainModelNode.name(), mainModelNode.partitions(), mainModelNode.dimensions(), mainModelNode.measures(), mainModelNode.navigationMode(), mainModelNode.uniquenessMode(), mainModelNode.rules()));
        var referenceModelNode = referenceModel("rates", select(lit(1)).build()).dimension("year").measure(lit(10), "rate").build();
        assertEquals(referenceModelNode, referenceModel(referenceModelNode.name(), referenceModelNode.query(), referenceModelNode.dimensions(), referenceModelNode.measures(), referenceModelNode.navigationMode(), referenceModelNode.uniquenessMode()));
        var modelClauseNode = model().dimension("year").measure(lit(10), "sales").reference(referenceModel("rates", select(lit(1)).build()).dimension("year").measure("rate").build()).rule(cellTarget("sales", 2026), lit(20)).build();
        assertEquals(modelClauseNode, modelClause(modelClauseNode.returnRows(), modelClauseNode.references(), modelClauseNode.main()));
    }

    @Test
    void conciseHelpersPreserveNamesAndTypedSelectors() {
        var quoted = id("Sales Year", QuoteStyle.DOUBLE_QUOTE);
        var column = col(id("input"), quoted);
        assertEquals(modelColumn(column, quoted), modelColumn(column));
        assertEquals(modelColumn(col("source"), id("target")), modelColumn("source", "target"));
        var selector = cellCondition(col("year").isAny());
        assertEquals(cellCondition(isAny("year")), selector);
        assertEquals(cellCondition(isAny()), cellAny());
        assertNull(currentDimension().dimension());
        var qualified = cellRef(id("baseline"), id("sales"), selector);
        assertEquals(id("baseline"), qualified.model());
        assertEquals(List.of(selector), qualified.selectors());
        var expression = currentDimension("year");
        var cell = cellRef("sales", selector, expression, null);
        assertSame(selector, cell.selectors().get(0));
        assertSame(expression, ((CellSelector.Value) cell.selectors().get(1)).value());
        assertEquals(lit(null), ((CellSelector.Value) cell.selectors().get(2)).value());
        assertEquals(cell.selectors(), cellTarget("sales", selector, expression, null).addresses());
        assertThrows(IllegalArgumentException.class, () -> cellRef("sales", cellForValues("year", 2026)));
        assertThrows(IllegalArgumentException.class, () -> cellTarget("sales", tbl("input")));
    }
}
