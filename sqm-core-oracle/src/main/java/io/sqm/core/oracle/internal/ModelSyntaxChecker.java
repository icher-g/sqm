package io.sqm.core.oracle.internal;

import io.sqm.core.*;
import io.sqm.core.walk.RecursiveNodeVisitor;

/**
 * Oracle MODEL syntax-shape checks shared by parsing and rendering.
 * Numeric limits, generated-cell counts, templates, and query semantics belong
 * to {@code sqm-validate-oracle}; these checks do not evaluate expression values
 * or resolve names.
 */
public final class ModelSyntaxChecker {
    private ModelSyntaxChecker() {
    }

    /**
     * Checks a symbolic selector such as year_key BETWEEN 2025 AND 2027.
     *
     * @param predicate selector condition
     */
    public static void selector(Predicate predicate) {
        selector(predicate, true);
    }

    private static void selector(Predicate predicate, boolean bareWildcard) {
        switch (predicate) {
            case ComparisonPredicate p -> {
                scalar(p.lhs());
                scalar(p.rhs());
            }
            case BetweenPredicate p -> {
                if (p.symmetric()) fail("symbolic BETWEEN cannot use SYMMETRIC");
                scalar(p.value());
                scalar(p.lower());
                scalar(p.upper());
            }
            case InPredicate p -> {
                scalar(p.lhs());
                if (!(p.rhs() instanceof RowExpr row)) fail("symbolic IN requires scalar values, not a query or tuple list");
                else row.items().forEach(ModelSyntaxChecker::scalar);
            }
            case IsNullPredicate p -> scalar(p.expr());
            case IsAnyPredicate p -> {
                if (p.dimension() == null) {
                    if (!bareWildcard) fail("bare ANY must be the complete selector");
                }
                else
                    if (!(p.dimension() instanceof ColumnExpr column) || column.tableAlias() != null) {
                        fail("IS ANY requires an unqualified dimension");
                    }
            }
            case LikePredicate p -> {
                if (p.mode() != LikeMode.LIKE) fail("symbolic pattern matching requires LIKE");
                scalar(p.value());
                scalar(p.pattern());
                if (p.escape() != null) scalar(p.escape());
            }
            case AndPredicate p -> {
                selector(p.lhs(), false);
                selector(p.rhs(), false);
            }
            case OrPredicate p -> {
                selector(p.lhs(), false);
                selector(p.rhs(), false);
            }
            case NotPredicate p -> selector(p.inner(), false);
            default -> fail("unsupported symbolic selector predicate: " + predicate.getTopLevelInterface().getSimpleName());
        }
    }

    /**
     * Rejects scalar subqueries and predicate-valued positional coordinates.
     *
     * @param expression positional coordinate, range expression, or explicit value
     */
    public static void scalar(Expression expression) {
        if (expression instanceof Predicate) fail("a positional value cannot be a predicate");
        if (expression instanceof RowExpr || expression instanceof RowListExpr) fail("a scalar MODEL expression cannot be a tuple");
        noSubqueries(expression);
    }

    private static void noSubqueries(Node node) {
        node.accept(new RecursiveNodeVisitor<Void>() {
            /** {@inheritDoc} */
            @Override
            protected Void defaultResult() {
                return null;
            }

            /** {@inheritDoc} */
            @Override
            public Void visitQueryExpr(QueryExpr node) {
                fail("subqueries are only allowed as FOR value sources or reference models");
                return null;
            }
        });
    }

    /**
     * Checks scalar expressions in explicit FOR lists; query sources retain ordinary query grammar.
     *
     * @param values FOR value generator
     */
    public static void values(CellFor.Values values) {
        switch (values.values()) {
            case RowExpr row -> row.items().forEach(ModelSyntaxChecker::scalar);
            case RowListExpr rows -> rows.rows().forEach(row -> row.items().forEach(ModelSyntaxChecker::scalar));
            case QueryExpr ignored -> {
            }
        }
    }

    /**
     * Checks scalar expression shapes in a FOR range without evaluating their values.
     *
     * @param range FOR range generator
     */
    public static void range(CellFor.Range range) {
        scalar(range.from());
        scalar(range.to());
        scalar(range.step());
        if (range.likePattern() != null) scalar(range.likePattern());
    }

    /**
     * Checks expression placement in {@code ITERATE (...) UNTIL (...)}.
     *
     * @param iteration iteration specification
     */
    public static void iteration(IterationSpec iteration) {
        scalar(iteration.limit());
        if (iteration.until() != null) noSubqueries(iteration.until());
    }

    /**
     * Rejects positional ordinals in a rule-local ORDER BY.
     *
     * @param order optional rule ordering
     */
    public static void orderBy(OrderBy order) {
        if (order != null && order.items().stream().anyMatch(item -> item.ordinal() != null)) {
            fail("rule ORDER BY cannot use positional ordinals");
        }
    }

    private static void fail(String message) {
        throw new IllegalArgumentException("MODEL " + message);
    }
}
