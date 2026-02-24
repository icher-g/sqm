package io.sqm.validate.schema.rule;

import io.sqm.core.ColumnExpr;
import io.sqm.core.Expression;
import io.sqm.core.FunctionExpr;
import io.sqm.core.Node;
import io.sqm.core.walk.RecursiveNodeVisitor;
import io.sqm.validate.schema.function.FunctionCatalog;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Aggregation-oriented expression analysis helpers.
 */
final class AggregationAnalysis {
    private final FunctionCatalog functionCatalog;

    /**
     * Creates aggregation analysis helpers backed by function catalog metadata.
     *
     * @param functionCatalog function catalog used to identify aggregate functions.
     */
    AggregationAnalysis(FunctionCatalog functionCatalog) {
        this.functionCatalog = functionCatalog;
    }

    /**
     * Returns whether expression contains aggregate function in non-window context.
     *
     * @param expression expression to inspect.
     * @return true if aggregate is present.
     */
    boolean containsAggregate(Expression expression) {
        var visitor = new AggregatePresenceVisitor(this);
        expression.accept(visitor);
        return visitor.found();
    }

    /**
     * Collects column references that appear outside aggregate function calls.
     *
     * @param node expression or predicate to inspect.
     * @return normalized column keys.
     */
    Set<String> nonAggregateColumnKeys(Node node) {
        var visitor = new NonAggregateColumnCollector(this);
        node.accept(visitor);
        return visitor.columnKeys();
    }

    /**
     * Builds normalized column key for matching grouped columns.
     *
     * @param column column expression.
     * @return normalized key.
     */
    static String columnKey(ColumnExpr column) {
        return normalize(column.tableAlias()) + "." + normalize(column.name());
    }

    boolean isAggregateFunction(FunctionExpr functionExpr) {
        if (functionExpr.over() != null) {
            return false;
        }
        if (functionExpr.filter() != null || functionExpr.withinGroup() != null) {
            return true;
        }
        return functionCatalog.resolve(functionName(functionExpr))
            .map(io.sqm.validate.schema.function.FunctionSignature::aggregate)
            .orElse(false);
    }

    private static String normalize(io.sqm.core.Identifier value) {
        return value == null ? "" : value.value().toLowerCase(Locale.ROOT);
    }

    private static String functionName(FunctionExpr functionExpr) {
        if (functionExpr == null || functionExpr.name() == null) {
            return null;
        }
        return functionExpr.name().parts().getLast().value();
    }

    /**
     * Visitor that detects aggregate function presence.
     */
    private static final class AggregatePresenceVisitor extends RecursiveNodeVisitor<Void> {
        private final AggregationAnalysis analysis;
        private boolean found;

        private AggregatePresenceVisitor(AggregationAnalysis analysis) {
            this.analysis = analysis;
        }

        boolean found() {
            return found;
        }

        @Override
        protected Void defaultResult() {
            return null;
        }

        @Override
        public Void visitFunctionExpr(FunctionExpr f) {
            if (analysis.isAggregateFunction(f)) {
                found = true;
            }
            return super.visitFunctionExpr(f);
        }
    }

    /**
     * Visitor that captures column references outside aggregate function boundaries.
     */
    private static final class NonAggregateColumnCollector extends RecursiveNodeVisitor<Void> {
        private final AggregationAnalysis analysis;
        private final Set<String> columnKeys = new HashSet<>();
        private int aggregateDepth;

        private NonAggregateColumnCollector(AggregationAnalysis analysis) {
            this.analysis = analysis;
        }

        Set<String> columnKeys() {
            return Set.copyOf(columnKeys);
        }

        @Override
        protected Void defaultResult() {
            return null;
        }

        @Override
        public Void visitFunctionExpr(FunctionExpr f) {
            if (analysis.isAggregateFunction(f)) {
                aggregateDepth++;
                try {
                    return super.visitFunctionExpr(f);
                } finally {
                    aggregateDepth--;
                }
            }
            return super.visitFunctionExpr(f);
        }

        @Override
        public Void visitColumnExpr(ColumnExpr c) {
            if (aggregateDepth == 0) {
                columnKeys.add(columnKey(c));
            }
            return super.visitColumnExpr(c);
        }
    }
}
