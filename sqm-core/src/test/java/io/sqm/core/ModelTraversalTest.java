package io.sqm.core;

import io.sqm.core.transform.RecursiveNodeTransformer;
import io.sqm.core.walk.RecursiveNodeVisitor;
import org.junit.jupiter.api.Test;
import java.util.*;
import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class ModelTraversalTest {
    static List<Node> nodes() {
        return List.of(
            modelColumn("country"),
            modelColumn("year"),
            modelColumn(lit(10), "sales"),
            cellRef("sales", 2026),
            cellTarget("sales", 2026),
            modelAggregateExpr(func("SUM", col("sales")), List.of(cellAny())),
            currentDimension("year"),
            iterationNumber(),
            previousModelValueExpr(cellRef("sales", 2026)),
            presenceValue(cellRef("sales", 2026), lit(10), lit(0)),
            cellIsPresent(cellRef("sales", 2026)),
            iterationSpec(lit(10), cellRef("sales", 2026).gt(lit(0))),
            modelRule(RuleMode.UPDATE, cellTarget("sales", 2026), OrderBy.of(List.of(col("year").asc())), lit(20)),
            modelRules().rule(cellTarget("sales", 2026), lit(20)).iteration(iterationSpec(lit(2), null)).build(),
            mainModel().dimension("year").measure(lit(10), "sales").rule(cellTarget("sales", 2026), lit(20)).build(),
            referenceModel("rates", select(lit(1)).build()).dimension("year").measure(lit(10), "rate").build(),
            model().dimension("year").measure(lit(10), "sales").reference(referenceModel("rates", select(lit(1)).build()).dimension("year").measure("rate").build()).rule(cellTarget("sales", 2026), lit(20)).build(),
            cellValue(lit(2026)),
            cellCondition(col("year").gt(2020)),
            isAny("year"),
            cellForValues(List.of(id("year"), id("product")), rows(row(2026, "Bike"))),
            cellForValues(List.of(id("year")), select(lit(2026)).build()),
            cellForRange(id("year"), lit("Y%"), lit(2020), lit(2026), RangeDirection.INCREMENT, lit(1))
        );
    }

    @Test
    void everyNodeDispatchesToItsDedicatedVisitorAndUnchangedTransformerPreservesIdentity() {
        var unchanged = new RecursiveNodeTransformer() {};
        var visited = new HashSet<String>();
        var visitor = new RecursiveNodeVisitor<Void>() {
            @Override protected Void defaultResult() { return null; }
            @Override public Void visitModelColumn(ModelColumn node) {
                visited.add("ModelColumn");
                return super.visitModelColumn(node);
            }
            @Override public Void visitCellRefExpr(CellRefExpr node) {
                visited.add("CellRefExpr");
                return super.visitCellRefExpr(node);
            }
            @Override public Void visitCellTarget(CellTarget node) {
                visited.add("CellTarget");
                return super.visitCellTarget(node);
            }
            @Override public Void visitModelAggregateExpr(ModelAggregateExpr node) {
                visited.add("ModelAggregateExpr");
                return super.visitModelAggregateExpr(node);
            }
            @Override public Void visitCurrentDimensionExpr(CurrentDimensionExpr node) {
                visited.add("CurrentDimensionExpr");
                return super.visitCurrentDimensionExpr(node);
            }
            @Override public Void visitIterationNumberExpr(IterationNumberExpr node) {
                visited.add("IterationNumberExpr");
                return super.visitIterationNumberExpr(node);
            }
            @Override public Void visitPreviousModelValueExpr(PreviousModelValueExpr node) {
                visited.add("PreviousModelValueExpr");
                return super.visitPreviousModelValueExpr(node);
            }
            @Override public Void visitPresenceValueExpr(PresenceValueExpr node) {
                visited.add("PresenceValueExpr");
                return super.visitPresenceValueExpr(node);
            }
            @Override public Void visitCellPresentPredicate(CellPresentPredicate node) {
                visited.add("CellPresentPredicate");
                return super.visitCellPresentPredicate(node);
            }
            @Override public Void visitIterationSpec(IterationSpec node) {
                visited.add("IterationSpec");
                return super.visitIterationSpec(node);
            }
            @Override public Void visitModelRule(ModelRule node) {
                visited.add("ModelRule");
                return super.visitModelRule(node);
            }
            @Override public Void visitModelRules(ModelRules node) {
                visited.add("ModelRules");
                return super.visitModelRules(node);
            }
            @Override public Void visitMainModel(MainModel node) {
                visited.add("MainModel");
                return super.visitMainModel(node);
            }
            @Override public Void visitReferenceModel(ReferenceModel node) {
                visited.add("ReferenceModel");
                return super.visitReferenceModel(node);
            }
            @Override public Void visitModelClause(ModelClause node) {
                visited.add("ModelClause");
                return super.visitModelClause(node);
            }
            @Override public Void visitCellSelectorValue(CellSelector.Value node) {
                visited.add("CellSelector.Value");
                return super.visitCellSelectorValue(node);
            }
            @Override public Void visitCellSelectorCondition(CellSelector.Condition node) {
                visited.add("CellSelector.Condition");
                return super.visitCellSelectorCondition(node);
            }
            @Override public Void visitCellForValues(CellFor.Values node) {
                visited.add("CellFor.Values");
                return super.visitCellForValues(node);
            }
            @Override public Void visitCellForRange(CellFor.Range node) {
                visited.add("CellFor.Range");
                return super.visitCellForRange(node);
            }
            @Override public Void visitIsAnyPredicate(IsAnyPredicate node) {
                visited.add("IsAnyPredicate");
                return super.visitIsAnyPredicate(node);
            }
        };
        for (var node : nodes()) {
            node.accept(visitor);
            assertSame(node, unchanged.transform(node), node.getClass().getName());
        }
        assertEquals(20, visited.size());
    }

    @Test
    void changingLiteralsRebuildsEveryContainingNodeButPreservesLeaves() {
        var transformer = new RecursiveNodeTransformer() {
            @Override public Node visitLiteralExpr(LiteralExpr literal) {
                return literal.value() instanceof Number number
                    ? lit(number.intValue() + 1) : lit(literal.value() + "_changed");
            }
        };
        for (var node : nodes()) {
            var literals = new ArrayList<LiteralExpr>();
            node.accept(new RecursiveNodeVisitor<Void>() {
                @Override protected Void defaultResult() { return null; }
                @Override public Void visitLiteralExpr(LiteralExpr literal) { literals.add(literal); return null; }
            });
            var result = transformer.transform(node);
            if (literals.isEmpty()) assertSame(node, result);
            else {
                assertNotSame(node, result, node.getClass().getName());
                assertNotEquals(node, result, node.getClass().getName());
            }
        }
    }

    @Test
    void queryCopyClearAndTransformRetainModelAndUnchangedSiblings() {
        var clause = model().dimension("year").measure(lit(10), "sales")
            .rule(cellTarget("sales", 2026), lit(20)).build();
        var query = select(col("sales")).from(tbl("input")).model(clause).build();
        assertSame(clause, SelectQuery.builder(query).build().model());
        assertEquals(query, SelectQuery.builder(query).build());
        assertNull(SelectQuery.builder(query).clearModel().build().model());
        assertSame(query, new RecursiveNodeTransformer() {}.transform(query));
        var changed = (SelectQuery) new RecursiveNodeTransformer() {
            @Override public Node visitMainModel(MainModel main) {
                var measures = main.measures().stream()
                    .map(column -> modelColumn(lit(30), column.name())).toList();
                return mainModel(main.name(), main.partitions(), main.dimensions(), measures,
                    main.navigationMode(), main.uniquenessMode(), main.rules());
            }
        }.transform(query);
        assertNotSame(query, changed);
        assertSame(query.from(), changed.from());
        assertSame(clause.main().rules(), changed.model().main().rules());
        assertSame(clause.main().dimensions().getFirst(), changed.model().main().dimensions().getFirst());
        assertEquals(lit(10), clause.main().measures().getFirst().expression());
        assertEquals(lit(30), changed.model().main().measures().getFirst().expression());
    }

    @Test
    void rewritingSourceColumnsPreservesResolvedNamesAndCellSelectors() {
        var rewrite = new RecursiveNodeTransformer() {
            @Override public Node visitColumnExpr(ColumnExpr column) {
                return col("renamed_source");
            }
        };
        var partition = modelColumn(col("country"), "region");
        var dimension = modelColumn(col("sales_year"), "year_key");
        var rewrittenPartition = (ModelColumn) rewrite.transform(partition);
        var rewrittenDimension = (ModelColumn) rewrite.transform(dimension);
        assertEquals(col("renamed_source"), rewrittenPartition.expression());
        assertEquals(partition.name(), rewrittenPartition.name());
        assertEquals(col("renamed_source"), rewrittenDimension.expression());
        assertEquals(dimension.name(), rewrittenDimension.name());
        var aggregate = modelAggregateExpr(func("SUM", col("sales")), List.of(cellAny()));
        var rewrittenAggregate = (ModelAggregateExpr) rewrite.transform(aggregate);
        assertEquals(func("SUM", col("renamed_source")), rewrittenAggregate.aggregate());
        assertSame(aggregate.selectors().getFirst(), rewrittenAggregate.selectors().getFirst());
    }
}
