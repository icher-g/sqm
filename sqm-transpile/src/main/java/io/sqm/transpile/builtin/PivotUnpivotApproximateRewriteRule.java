package io.sqm.transpile.builtin;

import io.sqm.core.*;
import io.sqm.core.dialect.SqlDialectId;
import io.sqm.transpile.RewriteFidelity;
import io.sqm.transpile.TranspileContext;
import io.sqm.transpile.TranspileRuleResult;
import io.sqm.transpile.rule.TranspileRule;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Rewrites {@code PIVOT}/{@code UNPIVOT} table transforms to portable query shapes.
 *
 * <p>{@code PIVOT} becomes conditional aggregation, while {@code UNPIVOT}
 * becomes a {@code UNION ALL} of branch SELECTs. The generated rewrite keeps
 * the pivot/unpivot desugaring in a derived table and preserves the original
 * outer query clauses around it.</p>
 */
public final class PivotUnpivotApproximateRewriteRule implements TranspileRule {
    /**
     * Creates a pivot/unpivot approximate rewrite rule.
     */
    public PivotUnpivotApproximateRewriteRule() {
    }

    private static TranspileRuleResult rewritePivot(SelectQuery query, PivotTable pivot) {
        if (pivot.measures().isEmpty()) {
            return unsupported(query, "PIVOT approximate rewrite requires at least one aggregate measure");
        }

        Map<String, PivotOutput> outputColumns = new LinkedHashMap<>();
        for (PivotValue value : pivot.values()) {
            for (PivotMeasure measure : pivot.measures()) {
                String outputName;
                try {
                    outputName = pivotOutputName(value, measure, pivot.measures().size());
                } catch (UnsupportedPivotRewriteException ex) {
                    return unsupported(query, ex.getMessage());
                }
                if (outputColumns.putIfAbsent(outputName, new PivotOutput(outputName, value, measure)) != null) {
                    return unsupported(query, "PIVOT approximate rewrite requires unique output column names");
                }
            }
        }

        List<GroupProjection> grouping;
        try {
            grouping = pivotGroupingProjections(query, pivot, outputColumns);
        } catch (UnsupportedPivotRewriteException ex) {
            return unsupported(query, ex.getMessage());
        }

        List<SelectItem> coreItems = new ArrayList<>();
        List<GroupItem> groupItems = new ArrayList<>();
        for (GroupProjection projection : grouping) {
            coreItems.add(ExprSelectItem.of(projection.expr(), projection.alias()));
            groupItems.add(GroupItem.of(projection.expr()));
        }

        for (PivotOutput output : outputColumns.values()) {
            try {
                coreItems.add(ExprSelectItem.of(
                    conditionalAggregate(output.measure(), pivot.forExpression(), output.value()),
                    Identifier.of(output.name())
                ));
            } catch (UnsupportedPivotRewriteException ex) {
                return unsupported(query, ex.getMessage());
            }
        }

        var core = SelectQuery.of(
            coreItems,
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
            List.of(),
            List.of()
        );

        var rewritten = wrapOuterQuery(query, core, pivot.alias(), "__pivot_rewrite");
        return approximate(rewritten, "Rewrote PIVOT to derived-table conditional aggregation");
    }

    private static TranspileRuleResult rewriteUnpivot(SelectQuery query, UnpivotTable unpivot) {
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
                    return unsupported(query, "UNPIVOT approximate rewrite still requires explicit expression SELECT items");
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
                List.of(),
                List.of()
            ));
        }

        var core = CompositeQuery.of(
            branches,
            java.util.Collections.nCopies(branches.size() - 1, SetOperator.UNION_ALL)
        );
        var rewritten = wrapOuterQuery(query, core, unpivot.alias(), "__unpivot_rewrite");
        return approximate(rewritten, "Rewrote UNPIVOT to derived-table UNION ALL");
    }

    private static SelectQuery wrapOuterQuery(SelectQuery original, Query core, Identifier alias, String fallbackAlias) {
        var tableAlias = alias == null ? Identifier.of(fallbackAlias) : alias;
        return SelectQuery.of(
            original.items(),
            QueryTable.of(core, tableAlias, List.of()),
            original.joins(),
            original.where(),
            original.hierarchical(),
            original.groupBy(),
            original.having(),
            original.orderBy(),
            original.distinct(),
            original.topSpec(),
            original.limitOffset(),
            original.lockFor(),
            original.windows(),
            original.modifiers(),
            original.hints()
        );
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

    private static List<GroupProjection> pivotGroupingProjections(
        SelectQuery query,
        PivotTable pivot,
        Map<String, PivotOutput> outputColumns) {

        var fromSource = groupingFromSourceProjection(pivot);
        if (!fromSource.isEmpty()) {
            return fromSource;
        }

        List<GroupProjection> result = new ArrayList<>();
        for (SelectItem item : query.items()) {
            if (!(item instanceof ExprSelectItem exprItem)) {
                continue;
            }
            if (selectedPivotOutput(exprItem, pivot, outputColumns).isPresent()) {
                continue;
            }

            var projection = pivotGroupingProjection(exprItem, pivot, !query.joins().isEmpty());
            projection.ifPresent(result::add);
        }

        if (result.isEmpty()) {
            throw new UnsupportedPivotRewriteException(
                "PIVOT rewrite cannot infer implicit grouping columns. Use an explicit source subquery projection or explicit outer SELECT items."
            );
        }
        return result;
    }

    private static Optional<GroupProjection> pivotGroupingProjection(
        ExprSelectItem item,
        PivotTable pivot,
        boolean hasOuterJoins) {

        if (item.expr() instanceof ColumnExpr column) {
            if (column.tableAlias() == null) {
                return Optional.of(new GroupProjection(column, outputAlias(item, column.name())));
            }
            if (sameIdentifier(column.tableAlias(), pivot.alias())) {
                return Optional.of(new GroupProjection(ColumnExpr.of(null, column.name()), outputAlias(item, column.name())));
            }
            return Optional.empty();
        }

        if (hasOuterJoins) {
            return Optional.empty();
        }

        return Optional.of(new GroupProjection(item.expr(), outputAlias(item, inferredAlias(item.expr()))));
    }

    private static List<GroupProjection> groupingFromSourceProjection(PivotTable pivot) {
        if (!(pivot.source() instanceof QueryTable table) || !(table.query() instanceof SelectQuery sourceQuery)) {
            return List.of();
        }

        var forName = columnName(pivot.forExpression());
        var measureNames = pivot.measures().stream()
            .map(PivotUnpivotApproximateRewriteRule::measureArgColumnName)
            .filter(name -> name != null)
            .collect(Collectors.toSet());
        List<GroupProjection> result = new ArrayList<>();
        for (SelectItem item : sourceQuery.items()) {
            if (!(item instanceof ExprSelectItem exprItem)) {
                return List.of();
            }
            var alias = outputAlias(exprItem, inferredAlias(exprItem.expr()));
            if (sameIdentifier(alias, forName) || measureNames.stream().anyMatch(name -> sameIdentifier(alias, name))) {
                continue;
            }
            result.add(new GroupProjection(ColumnExpr.of(null, alias), alias));
        }
        return result;
    }

    private static Identifier measureArgColumnName(PivotMeasure measure) {
        var aggregate = measure.aggregateFunction();
        if (aggregate.args().size() == 1 && aggregate.args().getFirst() instanceof FunctionExpr.Arg.ExprArg exprArg) {
            return columnName(exprArg.expr());
        }
        return null;
    }

    private static Identifier columnName(Expression expr) {
        return expr instanceof ColumnExpr column && column.tableAlias() == null ? column.name() : null;
    }

    private static Identifier inferredAlias(Expression expr) {
        return expr instanceof ColumnExpr column ? column.name() : null;
    }

    private static Optional<PivotOutput> selectedPivotOutput(ExprSelectItem item, PivotTable pivot, Map<String, PivotOutput> outputColumns) {
        if (item.expr() instanceof ColumnExpr column && (column.tableAlias() == null || sameIdentifier(column.tableAlias(), pivot.alias()))) {
            var name = normalize(column.name());
            var output = outputColumns.get(name);
            if (output != null) {
                return Optional.of(output);
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

    private static String pivotOutputName(PivotValue value, PivotMeasure measure, int measureCount) {
        var valueName = pivotValueOutputName(value);
        if (measureCount == 1) {
            return valueName;
        }
        return valueName + "_" + pivotMeasureOutputName(measure);
    }

    private static String pivotValueOutputName(PivotValue value) {
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

    private static String pivotMeasureOutputName(PivotMeasure measure) {
        if (measure.alias() != null) {
            return normalize(measure.alias());
        }
        return normalize(measure.aggregateFunction().name().parts().getLast());
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
     * @param context   transpilation context
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

    private record PivotOutput(String name, PivotValue value, PivotMeasure measure) {
    }

    private record GroupProjection(Expression expr, Identifier alias) {
    }

    private static final class UnsupportedPivotRewriteException extends RuntimeException {
        private UnsupportedPivotRewriteException(String message) {
            super(message);
        }
    }
}
