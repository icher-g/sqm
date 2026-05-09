package io.sqm.core.transform;

import io.sqm.core.ColumnExpr;
import io.sqm.core.ComparisonOperator;
import io.sqm.core.ComparisonPredicate;
import io.sqm.core.CompositeQuery;
import io.sqm.core.CteDef;
import io.sqm.core.Expression;
import io.sqm.core.ExprSelectItem;
import io.sqm.core.HierarchicalQueryClause;
import io.sqm.core.Identifier;
import io.sqm.core.Join;
import io.sqm.core.Node;
import io.sqm.core.Predicate;
import io.sqm.core.PriorExpr;
import io.sqm.core.SelectItem;
import io.sqm.core.SelectQuery;
import io.sqm.core.SetOperator;
import io.sqm.core.Table;
import io.sqm.core.WithQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Rewrites simple Oracle-style hierarchical queries into recursive common table expressions.
 * <p>
 * The supported shape is intentionally narrow: a single base table, projected columns
 * and optional {@code LEVEL}, and a {@code CONNECT BY PRIOR parent = child} equality.
 * More complex hierarchical semantics are rejected explicitly with
 * {@link UnsupportedRewriteException}.
 * </p>
 */
public final class HierarchicalQueryToRecursiveCteTransformer extends RecursiveNodeTransformer {
    private static final Identifier CTE_NAME = Identifier.of("sqm_tree");
    private static final Identifier CHILD_ALIAS = Identifier.of("sqm_child");
    private static final Identifier PARENT_ALIAS = Identifier.of("sqm_parent");
    private static final Identifier LEVEL_ALIAS = Identifier.of("level");

    /**
     * Creates a hierarchical-query-to-recursive-CTE transformer.
     */
    public HierarchicalQueryToRecursiveCteTransformer() {
    }

    /**
     * Rewrites a single hierarchical {@link SelectQuery} into a recursive CTE.
     *
     * @param query hierarchical query to rewrite
     * @return recursive CTE query
     * @throws UnsupportedRewriteException when the query shape cannot be rewritten exactly
     */
    public WithQuery rewrite(SelectQuery query) {
        Objects.requireNonNull(query, "query");
        var hierarchical = query.hierarchical();
        if (hierarchical == null) {
            throw new UnsupportedRewriteException("No hierarchical query detected");
        }
        return rewrite(query, hierarchical);
    }

    /**
     * Rewrites supported hierarchical {@link SelectQuery} nodes during recursive transformation.
     *
     * @param query select query to visit
     * @return recursive CTE when hierarchical, otherwise the default recursive transformation result
     */
    @Override
    public Node visitSelectQuery(SelectQuery query) {
        if (query.hierarchical() == null) {
            return super.visitSelectQuery(query);
        }
        return rewrite(query, query.hierarchical());
    }

    private static WithQuery rewrite(SelectQuery query, HierarchicalQueryClause hierarchical) {
        validateQueryShape(query, hierarchical);

        var source = (Table) query.from();
        var sourceQualifier = sourceQualifier(source);
        var childTable = alias(source, CHILD_ALIAS);
        var parentCte = Table.of(CTE_NAME).as(PARENT_ALIAS);
        var projection = buildProjection(query.items(), sourceQualifier);
        var connectBy = extractConnectBy(hierarchical.connectBy(), sourceQualifier);

        var anchor = SelectQuery.builder()
            .select(projection.anchorItems())
            .from(childTable)
            .where(qualify(hierarchical.startWith(), sourceQualifier, CHILD_ALIAS))
            .build();

        var recursive = SelectQuery.builder()
            .select(projection.recursiveItems())
            .from(childTable)
            .join(Join.inner(parentCte).on(connectBy.toJoinPredicate()))
            .build();

        var cteBody = CompositeQuery.of(List.of(anchor, recursive), List.of(SetOperator.UNION_ALL));
        var cte = CteDef.of(CTE_NAME, cteBody, projection.columnAliases());
        var outer = SelectQuery.builder()
            .select(projection.outerItems())
            .from(Table.of(CTE_NAME))
            .build();

        return WithQuery.of(List.of(cte), outer, true);
    }

    private static void validateQueryShape(SelectQuery query, HierarchicalQueryClause hierarchical) {
        if (!(query.from() instanceof Table)) {
            throw new UnsupportedRewriteException("Hierarchical query rewrite requires a single base table");
        }
        if (!query.joins().isEmpty()) {
            throw new UnsupportedRewriteException("Hierarchical query rewrite does not support joins");
        }
        if (query.where() != null) {
            throw new UnsupportedRewriteException("Hierarchical query rewrite does not support SELECT WHERE yet");
        }
        if (query.groupBy() != null || query.having() != null || !query.windows().isEmpty()) {
            throw new UnsupportedRewriteException("Hierarchical query rewrite does not support grouping, HAVING, or WINDOW clauses");
        }
        if (query.orderBy() != null || query.limitOffset() != null || query.lockFor() != null) {
            throw new UnsupportedRewriteException("Hierarchical query rewrite does not support ORDER BY, pagination, or locking clauses");
        }
        if (query.distinct() != null || query.topSpec() != null || !query.modifiers().isEmpty() || !query.hints().isEmpty()) {
            throw new UnsupportedRewriteException("Hierarchical query rewrite does not support SELECT modifiers, hints, DISTINCT, or TOP");
        }
        if (hierarchical.noCycle()) {
            throw new UnsupportedRewriteException("CONNECT BY NOCYCLE rewrite is not supported yet");
        }
        if (hierarchical.orderSiblingsBy() != null) {
            throw new UnsupportedRewriteException("ORDER SIBLINGS BY rewrite is not supported yet");
        }
    }

    private static Projection buildProjection(List<SelectItem> items, Identifier sourceQualifier) {
        var anchorItems = new ArrayList<SelectItem>(items.size());
        var recursiveItems = new ArrayList<SelectItem>(items.size());
        var outerItems = new ArrayList<SelectItem>(items.size());
        var columnAliases = new ArrayList<Identifier>(items.size());

        for (var item : items) {
            if (!(item instanceof ExprSelectItem exprItem)) {
                throw new UnsupportedRewriteException("Hierarchical query rewrite supports expression select items only");
            }

            var expr = exprItem.expr();
            if (isLevel(expr)) {
                var alias = exprItem.alias() == null ? LEVEL_ALIAS : exprItem.alias();
                columnAliases.add(alias);
                anchorItems.add(Expression.literal(1).as(alias));
                recursiveItems.add(ColumnExpr.of(PARENT_ALIAS, alias).add(Expression.literal(1)).as(alias));
                outerItems.add(ColumnExpr.of(CTE_NAME, alias).as(exprItem.alias()));
            }
            else if (expr instanceof ColumnExpr column) {
                assertSourceColumn(column, sourceQualifier);
                var alias = exprItem.alias() == null ? column.name() : exprItem.alias();
                columnAliases.add(alias);
                anchorItems.add(ColumnExpr.of(CHILD_ALIAS, column.name()).as(alias));
                recursiveItems.add(ColumnExpr.of(CHILD_ALIAS, column.name()).as(alias));
                outerItems.add(ColumnExpr.of(CTE_NAME, alias).as(exprItem.alias()));
            }
            else {
                throw new UnsupportedRewriteException("Hierarchical query rewrite supports projected columns and LEVEL only");
            }
        }

        return new Projection(anchorItems, recursiveItems, outerItems, columnAliases);
    }

    private static ConnectByRelation extractConnectBy(Predicate predicate, Identifier sourceQualifier) {
        if (!(predicate instanceof ComparisonPredicate comparison) || comparison.operator() != ComparisonOperator.EQ) {
            throw new UnsupportedRewriteException("CONNECT BY rewrite requires an equality predicate");
        }

        var lhsPrior = priorColumn(comparison.lhs(), sourceQualifier);
        var rhsPrior = priorColumn(comparison.rhs(), sourceQualifier);
        if (lhsPrior != null && rhsPrior != null) {
            throw new UnsupportedRewriteException("CONNECT BY rewrite requires PRIOR on exactly one side");
        }
        if (lhsPrior != null && comparison.rhs() instanceof ColumnExpr child) {
            assertSourceColumn(child, sourceQualifier);
            return new ConnectByRelation(lhsPrior, child);
        }
        if (rhsPrior != null && comparison.lhs() instanceof ColumnExpr child) {
            assertSourceColumn(child, sourceQualifier);
            return new ConnectByRelation(rhsPrior, child);
        }

        throw new UnsupportedRewriteException("CONNECT BY rewrite requires PRIOR column = child column");
    }

    private static ColumnExpr priorColumn(Expression expr, Identifier sourceQualifier) {
        if (!(expr instanceof PriorExpr prior)) {
            return null;
        }
        if (!(prior.expr() instanceof ColumnExpr column)) {
            throw new UnsupportedRewriteException("CONNECT BY rewrite supports PRIOR column references only");
        }
        assertSourceColumn(column, sourceQualifier);
        return column;
    }

    private static Predicate qualify(Predicate predicate, Identifier sourceQualifier, Identifier newQualifier) {
        if (predicate == null) {
            return null;
        }
        return new ColumnQualifier(sourceQualifier, newQualifier).rewrite(predicate);
    }

    private static void assertSourceColumn(ColumnExpr column, Identifier sourceQualifier) {
        var tableAlias = column.tableAlias();
        if (tableAlias != null && !sameIdentifier(tableAlias, sourceQualifier)) {
            throw new UnsupportedRewriteException("Hierarchical query rewrite supports columns from the hierarchical source table only");
        }
    }

    private static boolean isLevel(Expression expr) {
        return expr instanceof ColumnExpr column
            && column.tableAlias() == null
            && "level".equalsIgnoreCase(column.name().value());
    }

    private static Identifier sourceQualifier(Table table) {
        return table.alias() == null ? table.name() : table.alias();
    }

    private static Table alias(Table table, Identifier alias) {
        return Table.of(table.schema(), table.name(), alias, table.inheritance(), table.hints());
    }

    private static boolean sameIdentifier(Identifier a, Identifier b) {
        if (a.quoteStyle() != b.quoteStyle()) {
            return false;
        }
        return a.quoted() ? a.value().equals(b.value()) : a.value().equalsIgnoreCase(b.value());
    }

    private record Projection(
        List<SelectItem> anchorItems,
        List<SelectItem> recursiveItems,
        List<SelectItem> outerItems,
        List<Identifier> columnAliases
    ) {
    }

    private record ConnectByRelation(ColumnExpr parentColumn, ColumnExpr childColumn) {
        private Predicate toJoinPredicate() {
            return ColumnExpr.of(PARENT_ALIAS, parentColumn.name())
                .eq(ColumnExpr.of(CHILD_ALIAS, childColumn.name()));
        }
    }

    private static final class ColumnQualifier extends RecursiveNodeTransformer {
        private final Identifier sourceQualifier;
        private final Identifier newQualifier;

        private ColumnQualifier(Identifier sourceQualifier, Identifier newQualifier) {
            this.sourceQualifier = sourceQualifier;
            this.newQualifier = newQualifier;
        }

        private Predicate rewrite(Predicate predicate) {
            return apply(predicate);
        }

        @Override
        public Node visitColumnExpr(ColumnExpr column) {
            assertSourceColumn(column, sourceQualifier);
            return ColumnExpr.of(newQualifier, column.name());
        }

        @Override
        public Node visitPriorExpr(PriorExpr prior) {
            throw new UnsupportedRewriteException("PRIOR is not supported inside START WITH");
        }
    }

    /**
     * Indicates that a hierarchical query shape cannot be rewritten exactly as a recursive CTE.
     */
    public static final class UnsupportedRewriteException extends RuntimeException {
        /**
         * Creates an unsupported rewrite exception.
         *
         * @param message diagnostic message describing the unsupported shape
         */
        public UnsupportedRewriteException(String message) {
            super(message);
        }
    }
}
