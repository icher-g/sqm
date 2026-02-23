package io.sqm.core.transform;

import io.sqm.core.ColumnExpr;
import io.sqm.core.CrossJoin;
import io.sqm.core.Join;
import io.sqm.core.Lateral;
import io.sqm.core.NaturalJoin;
import io.sqm.core.Node;
import io.sqm.core.OnJoin;
import io.sqm.core.Query;
import io.sqm.core.SelectQuery;
import io.sqm.core.Table;
import io.sqm.core.TableRef;
import io.sqm.core.UsingJoin;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/**
 * Qualifies unqualified {@link ColumnExpr} references using visible table bindings provided by a resolver.
 *
 * <p>The transformer inspects local SELECT {@code FROM}/{@code JOIN} sources, collects visible base tables
 * (alias if present, otherwise table name), and rewrites only unqualified columns when the resolver returns
 * a deterministic qualifier.</p>
 */
public final class ColumnQualificationTransformer extends RecursiveNodeTransformer {
    private final ColumnQualificationResolver resolver;
    private final Deque<List<VisibleTableBinding>> scopes = new ArrayDeque<>();

    private ColumnQualificationTransformer(ColumnQualificationResolver resolver) {
        this.resolver = resolver;
    }

    /**
     * Creates a new column qualification transformer.
     *
     * @param resolver column qualification resolver
     * @return transformer instance
     */
    public static ColumnQualificationTransformer of(ColumnQualificationResolver resolver) {
        return new ColumnQualificationTransformer(Objects.requireNonNull(resolver, "resolver"));
    }

    /**
     * Applies column qualification to a query tree.
     *
     * @param query query to transform
     * @return transformed query or original instance when unchanged
     */
    public Query apply(Query query) {
        return (Query) transform(query);
    }

    @Override
    public Node visitSelectQuery(SelectQuery q) {
        scopes.push(collectLocalScope(q));
        try {
            return super.visitSelectQuery(q);
        } finally {
            scopes.pop();
        }
    }

    @Override
    public Node visitColumnExpr(ColumnExpr c) {
        if (c.tableAlias() != null) {
            return c;
        }
        ColumnQualification result = resolver.resolve(c.name(), visibleTables());
        if (result instanceof ColumnQualification.Unresolved) {
            return c;
        }
        if (result instanceof ColumnQualification.Ambiguous) {
            throw new AmbiguousColumnQualificationException(c.name());
        }
        return c.inTable(((ColumnQualification.Qualified) result).qualifier());
    }

    private List<VisibleTableBinding> visibleTables() {
        if (scopes.isEmpty()) {
            return List.of();
        }
        ArrayList<VisibleTableBinding> visible = new ArrayList<>();
        for (List<VisibleTableBinding> scope : scopes) {
            visible.addAll(scope);
        }
        return List.copyOf(visible);
    }

    private static List<VisibleTableBinding> collectLocalScope(SelectQuery q) {
        ArrayList<VisibleTableBinding> visible = new ArrayList<>();
        addVisibleTable(visible, q.from());
        for (Join join : q.joins()) {
            switch (join) {
                case OnJoin onJoin -> addVisibleTable(visible, onJoin.right());
                case UsingJoin usingJoin -> addVisibleTable(visible, usingJoin.right());
                case CrossJoin crossJoin -> addVisibleTable(visible, crossJoin.right());
                case NaturalJoin naturalJoin -> addVisibleTable(visible, naturalJoin.right());
                default -> {
                    // Dialect joins are ignored unless they expose base tables through existing join nodes.
                }
            }
        }
        return List.copyOf(visible);
    }

    private static void addVisibleTable(List<VisibleTableBinding> visible, TableRef ref) {
        if (ref == null) {
            return;
        }
        switch (ref) {
            case Table table -> {
                String qualifier = table.alias() == null ? table.name() : table.alias();
                visible.add(VisibleTableBinding.of(table.schema(), table.name(), qualifier));
            }
            case Lateral lateral -> addVisibleTable(visible, lateral.inner());
            default -> {
                // Query/values/function tables are intentionally skipped here because no catalog mapping
                // is available in the generic core transformer.
            }
        }
    }

    /**
     * Thrown when a column cannot be qualified deterministically because multiple visible tables match.
     */
    public static final class AmbiguousColumnQualificationException extends RuntimeException {
        /**
         * Creates exception for ambiguous column name.
         *
         * @param columnName ambiguous column name
         */
        public AmbiguousColumnQualificationException(String columnName) {
            super("Ambiguous unqualified column: " + columnName);
        }
    }
}
