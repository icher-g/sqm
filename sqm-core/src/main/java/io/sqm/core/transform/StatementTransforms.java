package io.sqm.core.transform;

import io.sqm.core.*;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * Task-oriented helpers for adapting statements at runtime.
 * <p>
 * These utilities are intended for cases where a statement is authored once
 * during development and later receives environment-specific constraints such
 * as tenant filters or customer visibility predicates before execution.
 */
public final class StatementTransforms {
    private static final BooleanPredicateSimplifier BOOLEAN_SIMPLIFIER = new BooleanPredicateSimplifier();

    private StatementTransforms() {
    }

    /**
     * Appends a predicate to a statement {@code WHERE} clause using {@code AND}.
     * <p>
     * Example:
     * <pre>{@code
     * var rewritten = StatementTransforms.andWhere(
     *     query,
     *     col("tenant_id").eq(lit(42))
     * );
     * }</pre>
     *
     * @param statement statement to adapt
     * @param filter    predicate to append to {@code WHERE}
     * @return adapted statement, or the original instance when nothing changes
     * @throws IllegalArgumentException when the statement type is not supported
     */
    public static Statement andWhere(Statement statement, Predicate filter) {
        Objects.requireNonNull(statement, "statement");
        return switch (statement) {
            case SelectQuery select -> andWhere(select, filter);
            case UpdateStatement update -> andWhere(update, filter);
            case DeleteStatement delete -> andWhere(delete, filter);
            default -> throw new IllegalArgumentException("Unsupported statement type for WHERE injection: " + statement.getClass().getSimpleName());
        };
    }

    /**
     * Appends a predicate to a {@link SelectQuery} {@code WHERE} clause using {@code AND}.
     * <p>
     * Example:
     * <pre>{@code
     * var rewritten = StatementTransforms.andWhere(
     *     query,
     *     col("tenant_id").eq(lit(42))
     * );
     * }</pre>
     *
     * @param query  query to adapt
     * @param filter predicate to append to {@code WHERE}
     * @return adapted query, or the original instance when nothing changes
     */
    public static SelectQuery andWhere(SelectQuery query, Predicate filter) {
        Objects.requireNonNull(query, "query");
        Predicate where = mergeWhere(query.where(), filter);
        if (where == query.where()) {
            return query;
        }
        return SelectQuery.builder(query)
            .where(where)
            .build();
    }

    /**
     * Appends a predicate to an {@link UpdateStatement} {@code WHERE} clause using {@code AND}.
     * <p>
     * Example:
     * <pre>{@code
     * var rewritten = StatementTransforms.andWhere(
     *     update,
     *     col("tenant_id").eq(lit(42))
     * );
     * }</pre>
     *
     * @param statement statement to adapt
     * @param filter    predicate to append to {@code WHERE}
     * @return adapted statement, or the original instance when nothing changes
     */
    public static UpdateStatement andWhere(UpdateStatement statement, Predicate filter) {
        Objects.requireNonNull(statement, "statement");
        Predicate where = mergeWhere(statement.where(), filter);
        if (where == statement.where()) {
            return statement;
        }
        return UpdateStatement.builder(statement)
            .where(where)
            .build();
    }

    /**
     * Appends a predicate to a {@link DeleteStatement} {@code WHERE} clause using {@code AND}.
     * <p>
     * Example:
     * <pre>{@code
     * var rewritten = StatementTransforms.andWhere(
     *     delete,
     *     col("tenant_id").eq(lit(42))
     * );
     * }</pre>
     *
     * @param statement statement to adapt
     * @param filter    predicate to append to {@code WHERE}
     * @return adapted statement, or the original instance when nothing changes
     */
    public static DeleteStatement andWhere(DeleteStatement statement, Predicate filter) {
        Objects.requireNonNull(statement, "statement");
        Predicate where = mergeWhere(statement.where(), filter);
        if (where == statement.where()) {
            return statement;
        }
        return DeleteStatement.builder(statement)
            .where(where)
            .build();
    }

    /**
     * Appends a predicate to {@code WHERE} only when its conjuncts are not already present.
     * <p>
     * Example:
     * <pre>{@code
     * var rewritten = StatementTransforms.andWhereIfMissing(
     *     query,
     *     col("tenant_id").eq(lit(42)).and(col("active").eq(lit(true)))
     * );
     * }</pre>
     *
     * @param statement statement to adapt
     * @param filter    predicate to append when missing
     * @return adapted statement, or the original instance when nothing changes
     * @throws IllegalArgumentException when the statement type is not supported
     */
    public static Statement andWhereIfMissing(Statement statement, Predicate filter) {
        Objects.requireNonNull(statement, "statement");
        return switch (statement) {
            case SelectQuery select -> andWhereIfMissing(select, filter);
            case UpdateStatement update -> andWhereIfMissing(update, filter);
            case DeleteStatement delete -> andWhereIfMissing(delete, filter);
            default -> throw new IllegalArgumentException("Unsupported statement type for duplicate-aware WHERE injection: " + statement.getClass().getSimpleName());
        };
    }

    /**
     * Appends a predicate to a {@link SelectQuery} {@code WHERE} clause only when its conjuncts are not already present.
     *
     * @param query  query to adapt
     * @param filter predicate to append when missing
     * @return adapted query, or the original instance when nothing changes
     */
    public static SelectQuery andWhereIfMissing(SelectQuery query, Predicate filter) {
        Objects.requireNonNull(query, "query");
        Predicate where = mergeWhereDistinct(query.where(), filter);
        if (where == query.where()) {
            return query;
        }
        return SelectQuery.builder(query)
            .where(where)
            .build();
    }

    /**
     * Appends a predicate to an {@link UpdateStatement} {@code WHERE} clause only when its conjuncts are not already present.
     *
     * @param statement statement to adapt
     * @param filter    predicate to append when missing
     * @return adapted statement, or the original instance when nothing changes
     */
    public static UpdateStatement andWhereIfMissing(UpdateStatement statement, Predicate filter) {
        Objects.requireNonNull(statement, "statement");
        Predicate where = mergeWhereDistinct(statement.where(), filter);
        if (where == statement.where()) {
            return statement;
        }
        return UpdateStatement.builder(statement)
            .where(where)
            .build();
    }

    /**
     * Appends a predicate to a {@link DeleteStatement} {@code WHERE} clause only when its conjuncts are not already present.
     *
     * @param statement statement to adapt
     * @param filter    predicate to append when missing
     * @return adapted statement, or the original instance when nothing changes
     */
    public static DeleteStatement andWhereIfMissing(DeleteStatement statement, Predicate filter) {
        Objects.requireNonNull(statement, "statement");
        Predicate where = mergeWhereDistinct(statement.where(), filter);
        if (where == statement.where()) {
            return statement;
        }
        return DeleteStatement.builder(statement)
            .where(where)
            .build();
    }

    /**
     * Appends resolver-driven predicates to {@code WHERE} for visible tables in this statement and nested query blocks.
     * <p>
     * Example:
     * <pre>{@code
     * var rewritten = StatementTransforms.andWherePerTableRecursively(
     *     query,
     *     binding -> "u".equals(binding.qualifier().value())
     *         ? col("u", "tenant_id").eq(lit(42))
     *         : null
     * );
     * }</pre>
     *
     * @param statement statement to adapt
     * @param resolver  resolver that contributes predicates per visible table
     * @return adapted statement, or the original instance when nothing changes
     */
    public static Statement andWherePerTableRecursively(Statement statement, Function<VisibleTableBinding, Predicate> resolver) {
        Objects.requireNonNull(statement, "statement");
        Objects.requireNonNull(resolver, "resolver");
        return new TableFilterInjector(resolver).transform(statement);
    }

    /**
     * Appends resolver-driven predicates to {@code WHERE} for visible tables in this query and nested query blocks.
     *
     * @param query    query to adapt
     * @param resolver resolver that contributes predicates per visible table
     * @return adapted query, or the original instance when nothing changes
     */
    public static Query andWherePerTableRecursively(Query query, Function<VisibleTableBinding, Predicate> resolver) {
        Objects.requireNonNull(query, "query");
        Objects.requireNonNull(resolver, "resolver");
        return (Query) new TableFilterInjector(resolver).transform(query);
    }

    /**
     * Appends resolver-driven predicates to {@code WHERE} for visible tables in the current statement block only.
     * <p>
     * Example:
     * <pre>{@code
     * var rewritten = StatementTransforms.andWherePerTable(
     *     query,
     *     binding -> switch (binding.qualifier().value()) {
     *         case "u" -> col("u", "tenant_id").eq(lit(42));
     *         case "o" -> col("o", "tenant_id").eq(lit(42));
     *         default -> null;
     *     }
     * );
     * }</pre>
     *
     * @param statement statement to adapt
     * @param resolver  resolver that contributes predicates per visible table in the current block
     * @return adapted statement, or the original instance when nothing changes
     */
    public static Statement andWherePerTable(Statement statement, Function<VisibleTableBinding, Predicate> resolver) {
        Objects.requireNonNull(statement, "statement");
        Objects.requireNonNull(resolver, "resolver");
        return switch (statement) {
            case SelectQuery select -> andWherePerTable(select, resolver);
            case UpdateStatement update -> andWherePerTable(update, resolver);
            case DeleteStatement delete -> andWherePerTable(delete, resolver);
            default -> throw new IllegalArgumentException(
                "Unsupported statement type for per-table WHERE injection: " + statement.getClass().getSimpleName()
            );
        };
    }

    /**
     * Appends resolver-driven predicates to {@code WHERE} for visible tables in the current query block only.
     *
     * @param query    query to adapt
     * @param resolver resolver that contributes predicates per visible table in the current block
     * @return adapted query, or the original instance when nothing changes
     */
    public static Query andWherePerTable(Query query, Function<VisibleTableBinding, Predicate> resolver) {
        Objects.requireNonNull(query, "query");
        Objects.requireNonNull(resolver, "resolver");
        if (query instanceof SelectQuery select) {
            return andWherePerTable(select, resolver);
        }
        throw new IllegalArgumentException("Unsupported query type for per-table WHERE injection: " + query.getClass().getSimpleName());
    }

    /**
     * Appends resolver-driven predicates to a {@link SelectQuery} {@code WHERE} clause for visible tables in the current block only.
     *
     * @param query    query to adapt
     * @param resolver resolver that contributes predicates per visible table in the current block
     * @return adapted query, or the original instance when nothing changes
     */
    public static SelectQuery andWherePerTable(SelectQuery query, Function<VisibleTableBinding, Predicate> resolver) {
        Objects.requireNonNull(query, "query");
        Objects.requireNonNull(resolver, "resolver");
        return injectLocalFilters(query, resolver);
    }

    /**
     * Appends resolver-driven predicates to an {@link UpdateStatement} {@code WHERE} clause for visible tables in the current block only.
     *
     * @param statement statement to adapt
     * @param resolver  resolver that contributes predicates per visible table in the current block
     * @return adapted statement, or the original instance when nothing changes
     */
    public static UpdateStatement andWherePerTable(UpdateStatement statement, Function<VisibleTableBinding, Predicate> resolver) {
        Objects.requireNonNull(statement, "statement");
        Objects.requireNonNull(resolver, "resolver");
        return injectLocalFilters(statement, resolver);
    }

    /**
     * Appends resolver-driven predicates to a {@link DeleteStatement} {@code WHERE} clause for visible tables in the current block only.
     *
     * @param statement statement to adapt
     * @param resolver  resolver that contributes predicates per visible table in the current block
     * @return adapted statement, or the original instance when nothing changes
     */
    public static DeleteStatement andWherePerTable(DeleteStatement statement, Function<VisibleTableBinding, Predicate> resolver) {
        Objects.requireNonNull(statement, "statement");
        Objects.requireNonNull(resolver, "resolver");
        return injectLocalFilters(statement, resolver);
    }

    private static Predicate mergeWhere(Predicate current, Predicate filter) {
        Objects.requireNonNull(filter, "filter");
        if (isLiteralBoolean(filter, true)) {
            return current;
        }
        if (current == null) {
            return filter;
        }
        return simplify(current.and(filter));
    }

    private static Predicate mergeWhereDistinct(Predicate current, Predicate filter) {
        Objects.requireNonNull(filter, "filter");
        var simplifiedFilter = simplify(filter);
        if (isLiteralBoolean(simplifiedFilter, true)) {
            return current;
        }
        if (current == null) {
            return simplifiedFilter;
        }
        var existing = conjunctionTerms(current);
        Predicate merged = current;
        boolean changed = false;
        for (var term : conjunctionTerms(simplifiedFilter)) {
            if (existing.contains(term)) {
                continue;
            }
            merged = simplify(merged.and(term));
            existing.add(term);
            changed = true;
        }
        return changed ? merged : current;
    }

    private static Predicate simplify(Predicate predicate) {
        return (Predicate) BOOLEAN_SIMPLIFIER.transform(predicate);
    }

    private static boolean isLiteralBoolean(Predicate predicate, boolean expected) {
        if (!(predicate instanceof UnaryPredicate unary)) {
            return false;
        }
        if (!(unary.expr() instanceof LiteralExpr literal)) {
            return false;
        }
        return literal.value() instanceof Boolean value && value == expected;
    }

    private static Set<Predicate> conjunctionTerms(Predicate predicate) {
        Set<Predicate> terms = new LinkedHashSet<>();
        collectConjunctionTerms(predicate, terms);
        return terms;
    }

    private static void collectConjunctionTerms(Predicate predicate, Set<Predicate> terms) {
        if (predicate instanceof AndPredicate and) {
            collectConjunctionTerms(and.lhs(), terms);
            collectConjunctionTerms(and.rhs(), terms);
            return;
        }
        terms.add(predicate);
    }

    private static SelectQuery injectLocalFilters(SelectQuery query, Function<VisibleTableBinding, Predicate> resolver) {
        var filter = resolveFilters(TableFilterInjector.collectVisibleTables(query), resolver);
        if (filter == null) {
            return query;
        }
        var where = mergeWhere(query.where(), filter);
        if (where == query.where()) {
            return query;
        }
        return SelectQuery.builder(query)
            .where(where)
            .build();
    }

    private static UpdateStatement injectLocalFilters(UpdateStatement statement, Function<VisibleTableBinding, Predicate> resolver) {
        var filter = resolveFilters(TableFilterInjector.collectVisibleTables(statement), resolver);
        if (filter == null) {
            return statement;
        }
        var where = mergeWhere(statement.where(), filter);
        if (where == statement.where()) {
            return statement;
        }
        return UpdateStatement.builder(statement)
            .where(where)
            .build();
    }

    private static DeleteStatement injectLocalFilters(DeleteStatement statement, Function<VisibleTableBinding, Predicate> resolver) {
        var filter = resolveFilters(TableFilterInjector.collectVisibleTables(statement), resolver);
        if (filter == null) {
            return statement;
        }
        var where = mergeWhere(statement.where(), filter);
        if (where == statement.where()) {
            return statement;
        }
        return DeleteStatement.builder(statement)
            .where(where)
            .build();
    }

    private static Predicate resolveFilters(List<VisibleTableBinding> visibleTables, Function<VisibleTableBinding, Predicate> resolver) {
        Predicate combined = null;
        for (var binding : visibleTables) {
            var next = resolver.apply(binding);
            if (next == null || isLiteralBoolean(next, true)) {
                continue;
            }
            combined = combined == null ? next : simplify(combined.and(next));
        }
        return combined;
    }

    private static final class TableFilterInjector extends RecursiveNodeTransformer {
        private final Function<VisibleTableBinding, Predicate> resolver;

        private TableFilterInjector(Function<VisibleTableBinding, Predicate> resolver) {
            this.resolver = resolver;
        }

        private static List<VisibleTableBinding> collectVisibleTables(SelectQuery query) {
            Set<VisibleTableBinding> visible = new LinkedHashSet<>();
            addVisibleTable(visible, query.from());
            for (Join join : query.joins()) {
                switch (join) {
                    case OnJoin onJoin -> addVisibleTable(visible, onJoin.right());
                    case UsingJoin usingJoin -> addVisibleTable(visible, usingJoin.right());
                    case CrossJoin crossJoin -> addVisibleTable(visible, crossJoin.right());
                    case NaturalJoin naturalJoin -> addVisibleTable(visible, naturalJoin.right());
                    default -> {
                    }
                }
            }
            return List.copyOf(visible);
        }

        private static List<VisibleTableBinding> collectVisibleTables(UpdateStatement statement) {
            Set<VisibleTableBinding> visible = new LinkedHashSet<>();
            addVisibleTable(visible, statement.table());
            for (var from : statement.from()) {
                addVisibleTable(visible, from);
            }
            for (var join : statement.joins()) {
                switch (join) {
                    case OnJoin onJoin -> addVisibleTable(visible, onJoin.right());
                    case UsingJoin usingJoin -> addVisibleTable(visible, usingJoin.right());
                    case CrossJoin crossJoin -> addVisibleTable(visible, crossJoin.right());
                    case NaturalJoin naturalJoin -> addVisibleTable(visible, naturalJoin.right());
                    default -> {
                    }
                }
            }
            return List.copyOf(visible);
        }

        private static List<VisibleTableBinding> collectVisibleTables(DeleteStatement statement) {
            Set<VisibleTableBinding> visible = new LinkedHashSet<>();
            addVisibleTable(visible, statement.table());
            for (var using : statement.using()) {
                addVisibleTable(visible, using);
            }
            for (var join : statement.joins()) {
                switch (join) {
                    case OnJoin onJoin -> addVisibleTable(visible, onJoin.right());
                    case UsingJoin usingJoin -> addVisibleTable(visible, usingJoin.right());
                    case CrossJoin crossJoin -> addVisibleTable(visible, crossJoin.right());
                    case NaturalJoin naturalJoin -> addVisibleTable(visible, naturalJoin.right());
                    default -> {
                    }
                }
            }
            return List.copyOf(visible);
        }

        private static void addVisibleTable(Set<VisibleTableBinding> visible, TableRef ref) {
            if (ref == null) {
                return;
            }
            switch (ref) {
                case Table table -> {
                    var qualifier = table.alias() == null ? table.name() : table.alias();
                    visible.add(VisibleTableBinding.of(
                        table.schema() == null ? null : table.schema().value(),
                        table.name().value(),
                        qualifier
                    ));
                }
                case Lateral lateral -> addVisibleTable(visible, lateral.inner());
                default -> {
                }
            }
        }

        private Statement transform(Statement statement) {
            return apply(statement);
        }

        @Override
        public Node visitSelectQuery(SelectQuery query) {
            var transformed = (SelectQuery) super.visitSelectQuery(query);
            return injectLocalFilters(transformed, resolver);
        }

        @Override
        public Node visitUpdateStatement(UpdateStatement statement) {
            var transformed = (UpdateStatement) super.visitUpdateStatement(statement);
            return injectLocalFilters(transformed, resolver);
        }

        @Override
        public Node visitDeleteStatement(DeleteStatement statement) {
            var transformed = (DeleteStatement) super.visitDeleteStatement(statement);
            return injectLocalFilters(transformed, resolver);
        }
    }
}
