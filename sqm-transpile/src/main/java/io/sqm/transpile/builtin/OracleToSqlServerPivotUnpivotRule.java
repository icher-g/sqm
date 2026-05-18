package io.sqm.transpile.builtin;

import io.sqm.core.*;
import io.sqm.core.collect.TableRefsCollector;
import io.sqm.core.dialect.SqlDialectId;
import io.sqm.core.transform.RecursiveNodeTransformer;
import io.sqm.dsl.Dsl;
import io.sqm.transpile.RewriteFidelity;
import io.sqm.transpile.TranspileContext;
import io.sqm.transpile.TranspileRuleResult;
import io.sqm.transpile.rule.TranspileRule;

import java.util.*;
import java.util.stream.Collectors;

import static io.sqm.dsl.Dsl.*;

/**
 * Rewrites Oracle PIVOT and UNPIVOT constructs into SQL Server-compatible forms.
 */
public final class OracleToSqlServerPivotUnpivotRule implements TranspileRule {

    /**
     * Creates a rule instance.
     */
    public OracleToSqlServerPivotUnpivotRule() {
    }

    private static String getName(Expression expr) {
        return expr.<String>matchExpression()
            .literal(l -> (String) l.value())
            .column(c -> c.name().value())
            .orElse(null);
    }

    private static boolean sameValue(Expression expr1, Expression expr2) {
        var val1 = getName(expr1);
        var val2 = getName(expr2);
        if (val1 == null || val2 == null) {
            return val1 == null && val2 == null;
        }
        return val1.equals(val2);
    }

    private static String shortenTableName(String tableName) {
        return String.join("", Arrays.stream(tableName.split("_")).map(w -> String.valueOf(w.charAt(0))).toList());
    }

    private static String generateAlias(String alias, Set<String> occupiedAliases) {
        int index = 0;
        while (occupiedAliases.contains(alias)) {
            alias = alias + index++;
        }
        occupiedAliases.add(alias);
        return alias;
    }

    private static String getOrGenerateTableAlias(TableRef tableRef, Set<String> occupiedAliases) {
        return switch (tableRef) {
            case Table t -> t.alias() != null ? t.alias().value() : generateAlias(shortenTableName(t.name().value()), occupiedAliases);
            case FunctionTable t -> t.alias() != null ? t.alias().value() : generateAlias("ft", occupiedAliases);
            case JsonTableRef t -> t.alias() != null ? t.alias().value() : generateAlias("jt", occupiedAliases);
            case QueryTable t -> t.alias() != null ? t.alias().value() : generateAlias("qt", occupiedAliases);
            case ValuesTable t -> t.alias() != null ? t.alias().value() : generateAlias("vt", occupiedAliases);
            case Lateral l -> getOrGenerateTableAlias(l.inner(), occupiedAliases);
            case PivotTable t -> t.alias() != null ? t.alias().value() : generateAlias("pt", occupiedAliases);
            case UnpivotTable t -> t.alias() != null ? t.alias().value() : generateAlias("upt", occupiedAliases);
            case VariableTable t -> generateAlias(shortenTableName(t.name().value()), occupiedAliases);
            case DialectTableRef ignore -> generateAlias("ut", occupiedAliases);
        };
    }

    private static String getTableAlias(TableRef tableRef) {
        return switch (tableRef) {
            case Table t -> t.alias() != null ? t.alias().value() : null;
            case FunctionTable t -> t.alias() != null ? t.alias().value() : null;
            case JsonTableRef t -> t.alias() != null ? t.alias().value() : null;
            case QueryTable t -> t.alias() != null ? t.alias().value() : null;
            case ValuesTable t -> t.alias() != null ? t.alias().value() : null;
            case Lateral l -> getTableAlias(l.inner());
            case PivotTable t -> t.alias() != null ? t.alias().value() : null;
            case UnpivotTable t -> t.alias() != null ? t.alias().value() : null;
            case VariableTable ignore -> null;
            case DialectTableRef t -> t instanceof AliasedTableRef ? ((AliasedTableRef) t).alias().value() : null;
        };
    }

    private static SelectItem updateTable(SelectItem item, String table) {
        return switch (item) {
            case ExprSelectItem exprItem -> {
                if (exprItem.expr() instanceof ColumnExpr columnExpr) {
                    yield ColumnExpr.of(Identifier.of(table), columnExpr.name()).toSelectItem();
                }
                yield exprItem;
            }
            case QualifiedStarSelectItem ignore -> QualifiedStarSelectItem.of(Identifier.of(table));
            case StarSelectItem ignore -> QualifiedStarSelectItem.of(Identifier.of(table));
            default -> item;
        };
    }

    private static TableRef updateAlias(TableRef tableRef, String alias) {
        return switch (tableRef) {
            case Table t -> t.as(alias);
            case FunctionTable t -> t.as(alias);
            case JsonTableRef t -> t.as(alias);
            case QueryTable t -> t.as(alias);
            case ValuesTable t -> t.as(alias);
            case Lateral l -> Lateral.of(updateAlias(l.inner(), alias));
            case PivotTable t -> t.as(alias);
            case UnpivotTable t -> t.as(alias);
            case VariableTable t -> t;
            case DialectTableRef t -> t;
        };
    }

    private static PivotTable rewritePivot(PivotTable pivot) {
        var pivotValues = new ArrayList<PivotValue>();
        for (PivotValue pivotValue : pivot.values()) {
            var column = pivotValue.value().<ColumnExpr>matchExpression()
                .literal(l -> col(Dsl.id((String) l.value(), QuoteStyle.BRACKETS)))
                .orElse(null);
            pivotValues.add(PivotValue.of(column, null));
        }
        return PivotTable.of(pivot.source(), pivot.measures(), pivot.forExpression(), pivotValues, pivot.alias());
    }

    private static UnpivotTable rewriteUnpivot(UnpivotTable unpivot) {
        // In case of multiple source columns: (q1_amount, q1_count) AS 'Q1'
        // SQL server query needs to use CROSS APPLY
        if (unpivot.inputs().stream().anyMatch(i -> i.sourceColumns().size() > 1)) {
            return unpivot;
        }

        var inputs = new ArrayList<UnpivotInput>();
        for (UnpivotInput input : unpivot.inputs()) {
            inputs.add(UnpivotInput.of(input.sourceColumns(), input.label()));
        }
        return UnpivotTable.of(unpivot.source(), unpivot.valueColumns(), unpivot.nameColumn(), inputs, unpivot.nullTreatment());
    }

    private static SelectQuery rewriteSelectForPivot(SelectQuery query, PivotTable queryPivot, PivotTable rewrittenPivot) {
        var items = new ArrayList<SelectItem>();
        for (var item : query.items()) {
            if (item instanceof ExprSelectItem exprItem) {
                var columnName = getName(exprItem.expr());
                var originalPivotValue = queryPivot.values().stream().filter(v -> v.alias().value().equals(columnName)).findFirst().orElse(null);
                if (originalPivotValue == null) {
                    items.add(item);
                    continue;
                }

                var pivotValue = rewrittenPivot.values().stream().filter(v -> sameValue(originalPivotValue.value(), v.value())).findFirst().orElse(null);
                if (pivotValue == null) {
                    items.add(item);
                    continue;
                }
                var name = getName(pivotValue.value());
                var table = exprItem.expr().<Identifier>matchExpression().column(c -> c.tableAlias()).orElse(null);
                items.add(col(table, Dsl.id(name, QuoteStyle.BRACKETS)).as(columnName));
            }
            else {
                items.add(item);
            }
        }
        var builder = SelectQuery.builder(query)
            .clearSelect()
            .select(items)
            .from(rewrittenPivot);
        return builder.build();
    }

    private static SelectQuery rewriteSelectForUnpivot(SelectQuery query, UnpivotTable queryUnpivot, UnpivotTable rewrittenUnpivot) {
        var items = new ArrayList<SelectItem>();
        for (var item : query.items()) {
            if (item instanceof ExprSelectItem exprItem) {
                var columnName = getName(exprItem.expr());
                if (!queryUnpivot.nameColumn().value().equals(columnName)) {
                    items.add(item);
                    continue;
                }
                var table = exprItem.expr().<Identifier>matchExpression().column(c -> c.tableAlias()).orElse(null);
                var nameColumn = col(table, queryUnpivot.nameColumn());
                var whens = new WhenThen[queryUnpivot.inputs().size()];
                List<UnpivotInput> inputs = queryUnpivot.inputs();
                for (int i = 0; i < inputs.size(); i++) {
                    var input = inputs.get(i);
                    whens[i] = when(nameColumn.eq(lit(input.sourceColumns().getFirst().value()))).then(input.label());
                }
                items.add(kase(whens).as(columnName));
            }
            else {
                items.add(item);
            }
        }
        var builder = SelectQuery.builder(query)
            .clearSelect()
            .select(items)
            .from(rewrittenUnpivot);
        return builder.build();
    }

    /**
     * Rewrite Unpivot as CROSS APPLY.
     * <pre>
     *     {@code
     *     Oracle UNPIVOT:
     *     SELECT
     *         region,
     *         sales_amount,
     *         sales_count,
     *         quarter
     *     FROM sales_summary
     *     UNPIVOT (
     *         (sales_amount, sales_count)
     *         FOR quarter IN (
     *             (q1_amount, q1_count) AS 'Q1',
     *             (q2_amount, q2_count) AS 'Q2',
     *             (q3_amount, q3_count) AS 'Q3'
     *         )
     *     );
     *
     *     SQL Server CROSS APPLY:
     *     SELECT
     *         s.region,
     *         v.sales_amount,
     *         v.sales_count,
     *         v.quarter
     *     FROM sales_summary AS s
     *     CROSS APPLY (
     *         VALUES
     *             (s.q1_amount, s.q1_count, 'Q1'),
     *             (s.q2_amount, s.q2_count, 'Q2'),
     *             (s.q3_amount, s.q3_count, 'Q3')
     *     ) AS v(sales_amount, sales_count, quarter)
     *     WHERE v.sales_amount IS NOT NULL
     *        OR v.sales_count IS NOT NULL;
     *     }
     * </pre>
     *
     * @param query        an original select query.
     * @param queryUnpivot unpivot statement
     * @return rewritten select query with CROSS APPLY join.
     */
    private static SelectQuery rewriteSelectAsCrossApply(SelectQuery query, UnpivotTable queryUnpivot) {
        var collector = new TableRefsCollector();
        query.accept(collector);
        var tableRefs = collector.getTableRefs();
        var occupiedAliases = tableRefs.stream().map(t -> getTableAlias(t)).filter(t -> t != null).collect(Collectors.toSet());
        var items = new ArrayList<SelectItem>();
        var fromAlias = getOrGenerateTableAlias(queryUnpivot.source(), occupiedAliases);
        var crossApplyAlias = generateAlias("upt", occupiedAliases);

        // update aliases to all items
        for (var item : query.items()) {
            if (item instanceof ExprSelectItem exprItem) {
                var columnName = getName(exprItem.expr());
                if (queryUnpivot.nameColumn().value().equals(columnName) || queryUnpivot.valueColumns().stream().anyMatch(c -> c.value().equals(columnName))) {
                    items.add(updateTable(item, crossApplyAlias));
                    continue;
                }
            }
            items.add(updateTable(item, fromAlias));
        }

        // build cross apply join with column aliases
        var columnAliases = new ArrayList<>(queryUnpivot.valueColumns());
        columnAliases.add(queryUnpivot.nameColumn());

        var rows = new ArrayList<RowExpr>();
        queryUnpivot.inputs().forEach(input -> {
            var row = new ArrayList<>(input.sourceColumns().stream().map(c -> (Expression) ColumnExpr.of(Identifier.of(fromAlias), c)).toList());
            row.add(input.label());
            rows.add(RowExpr.of(row));
        });
        var crossApply = CrossJoin.of(Lateral.of(ValuesTable.of(RowListExpr.of(rows), Identifier.of(crossApplyAlias), columnAliases)));

        var where = query.where();

        // handle NULL treatment explicit and default Oracle behavior
        if (queryUnpivot.nullTreatment() == UnpivotTable.NullTreatment.DIALECT_DEFAULT || queryUnpivot.nullTreatment() == UnpivotTable.NullTreatment.EXCLUDE_NULLS) {
            Predicate predicate = null;
            for (var column : queryUnpivot.valueColumns()) {
                if (predicate == null) {
                    predicate = IsNullPredicate.of(ColumnExpr.of(Identifier.of(crossApplyAlias), column), true);
                    continue;
                }
                predicate = predicate.or(IsNullPredicate.of(ColumnExpr.of(Identifier.of(crossApplyAlias), column), true));
            }
            where = where == null ? predicate : where.and(predicate);
        }

        var builder = SelectQuery.builder(query)
            .clearSelect()
            .select(items)
            .from(updateAlias(queryUnpivot.source(), fromAlias))
            .join(crossApply)
            .where(where);

        return builder.build();
    }

    /**
     * Returns a stable rule identifier.
     *
     * @return rule identifier
     */
    @Override
    public String id() {
        return "oracle-to-sqlserver-pivot-unpivot-rule";
    }

    /**
     * Returns the default execution order for this dialect-specific rewrite.
     *
     * @return rule order
     */
    @Override
    public int order() {
        return -9;
    }

    /**
     * Returns supported source dialects.
     *
     * @return supported source dialects, or an empty set for runtime-only applicability
     */
    @Override
    public Set<SqlDialectId> sourceDialects() {
        return Set.of(SqlDialectId.ORACLE);
    }

    /**
     * Returns supported target dialects.
     *
     * @return supported target dialects, or an empty set for runtime-only applicability
     */
    @Override
    public Set<SqlDialectId> targetDialects() {
        return Set.of(SqlDialectId.SQLSERVER);
    }

    /**
     * Applies the rule to the current statement.
     *
     * @param statement current statement
     * @param context   transpilation context
     * @return rule result
     */
    @Override
    public TranspileRuleResult apply(Statement statement, TranspileContext context) {
        if (!StatementFeatureInspector.hasPivotOrUnpivotTable(statement)) {
            return TranspileRuleResult.unchanged(statement, "No PIVOT or UNPIVOT usage detected");
        }

        var transformer = new RecursiveNodeTransformer() {
            private final Stack<SelectQuery> stack = new Stack<>();
            private final Map<SelectQuery, PivotTable> pivotTables = new HashMap<>();
            private final Map<SelectQuery, UnpivotTable> unpivotTables = new HashMap<>();
            private RewriteFidelity fidelity = RewriteFidelity.EXACT;
            private String description = "PIVOT/UNPIVOT was rewritten exactly";

            @Override
            public Node visitSelectQuery(SelectQuery q) {
                try {
                    stack.push(q);
                    super.visitSelectQuery(q);
                    if (pivotTables.containsKey(q) && q.from() instanceof PivotTable queryPivot) {
                        return rewriteSelectForPivot(q, queryPivot, pivotTables.get(q));
                    }
                    if (q.from() instanceof UnpivotTable queryUnpivot) {
                        // If there is a rewritten unpivot table meaning that we have only single source column in each unpivot input.
                        if (unpivotTables.containsKey(q)) {
                            return rewriteSelectForUnpivot(q, queryUnpivot, unpivotTables.get(q));
                        }
                        fidelity = RewriteFidelity.APPROXIMATE;
                        description = "UNPIVOT was rewritten approximately with CROSS APPLY";
                        return rewriteSelectAsCrossApply(q, queryUnpivot);
                    }
                    return q;
                } finally {
                    stack.pop();
                    pivotTables.remove(q);
                    unpivotTables.remove(q);
                }
            }

            @Override
            public Node visitPivotTable(PivotTable t) {
                var query = stack.peek();
                var p = rewritePivot(t);
                if (p != t) {
                    pivotTables.put(query, p);
                    return p;
                }
                return t;
            }

            @Override
            public Node visitUnpivotTable(UnpivotTable t) {
                var query = stack.peek();
                var p = rewriteUnpivot(t);
                if (p != t) {
                    unpivotTables.put(query, p);
                    return p;
                }
                return t;
            }
        };

        var transformedStatement = (Statement) statement.accept(transformer);
        return new TranspileRuleResult(
            transformedStatement,
            true,
            transformer.fidelity,
            List.of(),
            List.of(),
            transformer.description
        );
    }
}
