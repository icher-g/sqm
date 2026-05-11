package io.sqm.transpile.builtin;

import io.sqm.core.CaseExpr;
import io.sqm.core.ColumnExpr;
import io.sqm.core.CompositeQuery;
import io.sqm.core.ExprSelectItem;
import io.sqm.core.Expression;
import io.sqm.core.FunctionExpr;
import io.sqm.core.GroupBy;
import io.sqm.core.GroupItem;
import io.sqm.core.Identifier;
import io.sqm.core.LiteralExpr;
import io.sqm.core.PivotMeasure;
import io.sqm.core.PivotTable;
import io.sqm.core.PivotValue;
import io.sqm.core.Query;
import io.sqm.core.SelectItem;
import io.sqm.core.SelectQuery;
import io.sqm.core.SetOperator;
import io.sqm.core.Statement;
import io.sqm.core.UnpivotInput;
import io.sqm.core.UnpivotTable;
import io.sqm.core.WhenThen;
import io.sqm.core.dialect.SqlDialectId;
import io.sqm.transpile.RewriteFidelity;
import io.sqm.transpile.TranspileContext;
import io.sqm.transpile.TranspileRuleResult;
import io.sqm.transpile.rule.TranspileRule;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Rewrites simple {@code PIVOT}/{@code UNPIVOT} table transforms to portable query shapes.
 *
 * <p>The rewrites are intentionally approximate and limited to explicit top-level
 * SELECT projections. {@code PIVOT} becomes conditional aggregation, while
 * {@code UNPIVOT} becomes a {@code UNION ALL} of branch SELECTs.</p>
 */
public final class PivotUnpivotApproximateRewriteRule implements TranspileRule {
    /**
     * Creates a pivot/unpivot approximate rewrite rule.
     */
    public PivotUnpivotApproximateRewriteRule() {
    }

    /**
     * Returns the stable rule identifier.
     *
     * @return rule identifier
     */
    @Override
    public String id() {
        return "pivot-unpivot-approximate-rewrite";
    }

    /**
     * Returns source dialects with native pivot/unpivot syntax.
     *
     * @return Oracle and SQL Server source dialects
     */
    @Override
    public Set<SqlDialectId> sourceDialects() {
        return Set.of(SqlDialectId.ORACLE, SqlDialectId.SQLSERVER);
    }

    /**
     * Returns target dialects without native SQM pivot/unpivot support.
     *
     * @return ANSI, PostgreSQL, and MySQL target dialects
     */
    @Override
    public Set<SqlDialectId> targetDialects() {
        return Set.of(SqlDialectId.ANSI, SqlDialectId.POSTGRESQL, SqlDialectId.MYSQL);
    }

    /**
     * Runs before target validation rejects pivot/unpivot.
     *
     * @return rule execution order
     */
    @Override
    public int order() {
        return -9;
    }

    /**
     * Applies approximate pivot/unpivot rewrites for simple top-level SELECT queries.
     *
     * @param statement statement to rewrite
     * @param context transpilation context
     * @return rewrite result
     */
    @Override
    public TranspileRuleResult apply(Statement statement, TranspileContext context) {
        if (!StatementFeatureInspector.hasPivotOrUnpivotTable(statement)) {
            return TranspileRuleResult.unchanged(statement, "No PIVOT or UNPIVOT usage detected");
        }
        if (!(statement instanceof SelectQuery query)) {
            return unsupported(statement, "PIVOT/UNPIVOT rewrites currently require a top-level SELECT query");
        }
        if (query.from() instanceof PivotTable pivot) {
            return rewritePivot(query, pivot);
        }
        if (query.from() instanceof UnpivotTable unpivot) {
            return rewriteUnpivot(query, unpivot);
        }
        return unsupported(statement, "Nested PIVOT/UNPIVOT table transforms are not supported for approximate transpilation");
    }

    private static TranspileRuleResult rewritePivot(SelectQuery query, PivotTable pivot) {
        var unsupported = unsupportedCommonQueryClauses(query);
        if (unsupported.isPresent()) {
            return unsupported(query, unsupported.get());
        }
        if (pivot.measures().size() != 1) {
            return unsupported(query, "PIVOT approximate rewrite currently supports exactly one aggregate measure");
        }

        var measure = pivot.measures().getFirst();
        Map<String, PivotValue> outputColumns = new LinkedHashMap<>();
        for (PivotValue value : pivot.values()) {
            String outputName;
            try {
                outputName = pivotOutputName(value);
            }
            catch (UnsupportedPivotRewriteException ex) {
                return unsupported(query, ex.getMessage());
            }
            if (outputColumns.putIfAbsent(outputName, value) != null) {
                return unsupported(query, "PIVOT approximate rewrite requires unique output column names");
            }
        }

        List<SelectItem> items = new ArrayList<>(query.items().size());
        List<GroupItem> groupItems = new ArrayList<>();
        for (SelectItem item : query.items()) {
            if (!(item instanceof ExprSelectItem exprItem)) {
                return unsupported(query, "PIVOT approximate rewrite requires explicit expression SELECT items");
            }
            var output = selectedPivotOutput(exprItem, pivot, outputColumns);
            if (output.isPresent()) {
                var alias = exprItem.alias() == null ? Identifier.of(output.get().name()) : exprItem.alias();
                try {
                    items.add(ExprSelectItem.of(conditionalAggregate(measure, pivot.forExpression(), output.get().value()), alias));
                }
                catch (UnsupportedPivotRewriteException ex) {
                    return unsupported(query, ex.getMessage());
                }
            }
            else {
                if (exprItem.expr() instanceof ColumnExpr column && sameIdentifier(column.tableAlias(), pivot.alias())) {
                    return unsupported(query, "PIVOT alias-qualified non-pivot columns cannot be rewritten without source alias metadata");
                }
                items.add(exprItem);
                groupItems.add(GroupItem.of(exprItem.expr()));
            }
        }

        var rewritten = SelectQuery.of(
            items,
            pivot.source(),
            List.of(),
            null,
            null,
            groupItems.isEmpty() ? null : GroupBy.of(groupItems),
            null,
            null,
            null,
            null,
            null,
            null,
            List.of(),
            query.modifiers(),
            query.hints()
        );
        return approximate(rewritten, "Rewrote PIVOT to conditional aggregation");
    }

    private static TranspileRuleResult rewriteUnpivot(SelectQuery query, UnpivotTable unpivot) {
        var unsupported = unsupportedCommonQueryClauses(query);
        if (unsupported.isPresent()) {
            return unsupported(query, unsupported.get());
        }
        if (unpivot.inputs().isEmpty()) {
            return unsupported(query, "UNPIVOT approximate rewrite requires at least one input branch");
        }
        if (unpivot.inputs().stream().anyMatch(input -> input.sourceColumns().size() != unpivot.valueColumns().size())) {
            return unsupported(query, "UNPIVOT approximate rewrite requires each input branch to match the output value column count");
        }

        List<Query> branches = new ArrayList<>(unpivot.inputs().size());
        for (UnpivotInput input : unpivot.inputs()) {
            List<SelectItem> branchItems = new ArrayList<>(query.items().size());
            for (SelectItem item : query.items()) {
                if (!(item instanceof ExprSelectItem exprItem)) {
                    return unsupported(query, "UNPIVOT approximate rewrite requires explicit expression SELECT items");
                }
                branchItems.add(rewriteUnpivotSelectItem(exprItem, unpivot, input));
            }
            branches.add(SelectQuery.of(
                branchItems,
                unpivot.source(),
                List.of(),
                unpivotNullFilter(unpivot, input),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                query.modifiers(),
                query.hints()
            ));
        }

        var rewritten = CompositeQuery.of(
            branches,
            java.util.Collections.nCopies(branches.size() - 1, SetOperator.UNION_ALL)
        );
        return approximate(rewritten, "Rewrote UNPIVOT to UNION ALL");
    }

    private static io.sqm.core.Predicate unpivotNullFilter(UnpivotTable unpivot, UnpivotInput input) {
        if (unpivot.nullTreatment() == UnpivotTable.NullTreatment.INCLUDE_NULLS) {
            return null;
        }
        io.sqm.core.Predicate filter = null;
        for (Identifier sourceColumn : input.sourceColumns()) {
            var predicate = ColumnExpr.of(null, sourceColumn).isNotNull();
            filter = filter == null ? predicate : filter.or(predicate);
        }
        return filter;
    }

    private static ExprSelectItem rewriteUnpivotSelectItem(ExprSelectItem item, UnpivotTable unpivot, UnpivotInput input) {
        if (item.expr() instanceof ColumnExpr column) {
            for (int i = 0; i < unpivot.valueColumns().size(); i++) {
                if (sameIdentifier(column.name(), unpivot.valueColumns().get(i))) {
                    return ExprSelectItem.of(ColumnExpr.of(column.tableAlias(), input.sourceColumns().get(i)), outputAlias(item, unpivot.valueColumns().get(i)));
                }
            }
            if (sameIdentifier(column.name(), unpivot.nameColumn())) {
                return ExprSelectItem.of(input.label(), outputAlias(item, unpivot.nameColumn()));
            }
        }
        return item;
    }

    private static Optional<String> unsupportedCommonQueryClauses(SelectQuery query) {
        if (!query.joins().isEmpty()) {
            return Optional.of("PIVOT/UNPIVOT approximate rewrite does not support outer joins around the table transform");
        }
        if (query.where() != null || query.groupBy() != null || query.having() != null) {
            return Optional.of("PIVOT/UNPIVOT approximate rewrite does not support existing WHERE, GROUP BY, or HAVING clauses");
        }
        if (query.hierarchical() != null || query.distinct() != null || query.topSpec() != null || query.limitOffset() != null || query.lockFor() != null) {
            return Optional.of("PIVOT/UNPIVOT approximate rewrite does not support additional SELECT modifiers");
        }
        if (query.orderBy() != null || !query.windows().isEmpty()) {
            return Optional.of("PIVOT/UNPIVOT approximate rewrite does not support ORDER BY or WINDOW clauses");
        }
        return Optional.empty();
    }

    private static Optional<PivotOutput> selectedPivotOutput(ExprSelectItem item, PivotTable pivot, Map<String, PivotValue> outputColumns) {
        if (item.expr() instanceof ColumnExpr column && (column.tableAlias() == null || sameIdentifier(column.tableAlias(), pivot.alias()))) {
            var name = normalize(column.name());
            var value = outputColumns.get(name);
            if (value != null) {
                return Optional.of(new PivotOutput(name, value));
            }
        }
        return Optional.empty();
    }

    private static Expression conditionalAggregate(PivotMeasure measure, Expression forExpression, PivotValue value) {
        var aggregate = measure.aggregateFunction();
        if (aggregate.orderBy() != null || aggregate.withinGroup() != null || aggregate.filter() != null || aggregate.over() != null) {
            throw new UnsupportedPivotRewriteException("PIVOT aggregate options cannot be rewritten approximately yet");
        }
        var arg = aggregate.args().size() == 1 && aggregate.args().getFirst() instanceof FunctionExpr.Arg.StarArg
            ? Expression.literal(1)
            : aggregate.args().size() == 1 && aggregate.args().getFirst() instanceof FunctionExpr.Arg.ExprArg exprArg
                ? exprArg.expr()
                : null;
        if (arg == null) {
            throw new UnsupportedPivotRewriteException("PIVOT approximate rewrite requires single-argument aggregate functions");
        }
        var condition = forExpression.eq(pivotComparisonValue(value.value()));
        var caze = CaseExpr.of(List.of(WhenThen.of(condition, arg)), Expression.literal(null));
        return FunctionExpr.of(
            aggregate.name(),
            List.of(FunctionExpr.Arg.expr(caze)),
            aggregate.distinctArg(),
            null,
            null,
            null,
            null
        );
    }

    private static Expression pivotComparisonValue(Expression value) {
        if (value instanceof ColumnExpr column && column.tableAlias() == null) {
            return Expression.literal(column.name().value());
        }
        return value;
    }

    private static String pivotOutputName(PivotValue value) {
        if (value.alias() != null) {
            return normalize(value.alias());
        }
        if (value.value() instanceof ColumnExpr column) {
            return normalize(column.name());
        }
        if (value.value() instanceof LiteralExpr literal && literal.value() != null) {
            return literal.value().toString().toLowerCase(Locale.ROOT);
        }
        throw new UnsupportedPivotRewriteException("PIVOT value without alias cannot be mapped to an output column name");
    }

    private static Identifier outputAlias(ExprSelectItem item, Identifier fallback) {
        return item.alias() == null ? fallback : item.alias();
    }

    private static boolean sameIdentifier(Identifier left, Identifier right) {
        return left != null && right != null && normalize(left).equals(normalize(right));
    }

    private static String normalize(Identifier identifier) {
        return identifier.value().toLowerCase(Locale.ROOT);
    }

    private static TranspileRuleResult approximate(Statement statement, String description) {
        return TranspileRuleResult.rewrittenWithWarning(
            statement,
            RewriteFidelity.APPROXIMATE,
            "APPROXIMATE_PIVOT_UNPIVOT_REWRITE",
            "PIVOT/UNPIVOT was rewritten approximately; column naming, grouping, and null behavior may differ from the source dialect",
            description
        );
    }

    private static TranspileRuleResult unsupported(Statement statement, String message) {
        return TranspileRuleResult.unsupported(statement, "UNSUPPORTED_PIVOT_UNPIVOT_REWRITE", message);
    }

    private record PivotOutput(String name, PivotValue value) {
    }

    private static final class UnsupportedPivotRewriteException extends RuntimeException {
        private UnsupportedPivotRewriteException(String message) {
            super(message);
        }
    }
}
