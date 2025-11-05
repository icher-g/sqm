package io.sqm.core.transform;

import io.sqm.core.*;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public abstract class RecursiveNodeTransformer implements NodeTransformer {

    /**
     * Apples the transformation logic and casts the result to the same type of the node.
     *
     * @param n   the node to transform.
     * @param <T> the actual node type.
     * @return a transformed node.
     */
    @SuppressWarnings("unchecked")
    protected <T extends Node> T apply(T n) {
        return (T) transform(n);
    }

    /**
     * Applies the transformation logic on a list of items.
     *
     * @param items            an original list of items to transform.
     * @param transformedItems a transformed list of items.
     * @param <T>              the type of the item.
     * @return True if at least one of the items has changed and False otherwise.
     */
    protected <T extends Node> boolean apply(List<T> items, List<T> transformedItems) {
        boolean changed = false;
        for (var item : items) {
            var transformed = apply(item);
            if (transformed != item) {
                changed = true;
                transformedItems.add(transformed);
            }
            else {
                transformedItems.add(item);
            }
        }
        return changed;
    }

    /**
     * Visits a {@link CaseExpr} node representing a {@code CASE WHEN ... THEN ... END} expression.
     *
     * @param c the case expression
     * @return a result produced by the visitor
     */
    @Override
    public Node visitCaseExpr(CaseExpr c) {
        List<WhenThen> whens = new ArrayList<>(c.whens().size());
        boolean changed = apply(c.whens(), whens);
        var elseExpr = apply(c.elseExpr());
        if (changed || elseExpr != c.elseExpr()) {
            return CaseExpr.of(whens, elseExpr);
        }
        return c;
    }

    /**
     * Visits a {@link ColumnExpr} node referencing a table column.
     *
     * @param c the column expression
     * @return a result produced by the visitor
     */
    @Override
    public Node visitColumnExpr(ColumnExpr c) {
        return c;
    }

    /**
     * Visits a {@link FunctionExpr} node representing a function call.
     *
     * @param f the function expression
     * @return a result produced by the visitor
     */
    @Override
    public Node visitFunctionExpr(FunctionExpr f) {
        List<FunctionExpr.Arg> args = new ArrayList<>();
        boolean changed = apply(f.args(), args);
        var withinGroup = apply(f.withinGroup());
        changed |= withinGroup != f.withinGroup();
        var filter = apply(f.filter());
        changed |= filter != f.filter();
        var over = apply(f.over());
        changed |= over != f.over();
        if (changed) {
            return FunctionExpr.of(f.name(), args, f.distinctArg(), withinGroup, filter, over);
        }
        return f;
    }

    /**
     * Visits a {@link FunctionExpr.Arg} node representing a single argument of a function call.
     *
     * @param a the function argument
     * @return a result produced by the visitor
     */
    @Override
    public Node visitFunctionArgExpr(FunctionExpr.Arg a) {
        if (a instanceof FunctionExpr.Arg.ExprArg e) {
            var expr = apply(e.expr());
            if (expr != e.expr()) {
                return FunctionExpr.Arg.expr(expr);
            }
            return e;
        }
        return a;
    }

    @Override
    public Node visitLiteralExpr(LiteralExpr l) {
        return l;
    }

    /**
     * Visits a {@link RowExpr} node representing a row value constructor
     * such as {@code (a, b, c)}.
     *
     * @param v the row expression
     * @return a result produced by the visitor
     */
    @Override
    public Node visitRowExpr(RowExpr v) {
        List<Expression> items = new ArrayList<>();
        if (apply(v.items(), items)) {
            return RowExpr.of(items);
        }
        return v;
    }

    /**
     * Visits a {@link QueryExpr} node representing a scalar subquery
     * used as an expression.
     *
     * @param v the query expression
     * @return a result produced by the visitor
     */
    @Override
    public Node visitQueryExpr(QueryExpr v) {
        var query = apply(v.subquery());
        if (query != v.subquery()) {
            return QueryExpr.of(query);
        }
        return v;
    }

    /**
     * Visits a {@link RowListExpr} node representing a list of row expressions,
     * for example {@code ((1,2),(3,4))} used in {@code IN} predicates or value sets.
     *
     * @param v the row list expression
     * @return a result produced by the visitor
     */
    @Override
    public Node visitRowListExpr(RowListExpr v) {
        List<RowExpr> rows = new ArrayList<>();
        if (apply(v.rows(), rows)) {
            return RowListExpr.of(rows);
        }
        return v;
    }

    /**
     * Visits an {@link ExprSelectItem}, representing a standard
     * projected expression, possibly with an alias.
     * Example: {@code LOWER(u.name) AS username}
     *
     * @param i the expression select item being visited (never {@code null})
     * @return a result value, or {@code null} if {@code <R>} is {@link Void}
     */
    @Override
    public Node visitExprSelectItem(ExprSelectItem i) {
        var expr = apply(i.expr());
        if (expr != i.expr()) {
            return ExprSelectItem.of(expr);
        }
        return i;
    }

    @Override
    public Node visitStarSelectItem(StarSelectItem i) {
        return i;
    }

    /**
     * Visits a {@link QualifiedStarSelectItem}, representing a
     * qualified {@code table.*} projection.
     * Example: {@code SELECT u.* FROM users u}
     *
     * @param i the qualified star select item being visited (never {@code null})
     * @return a result value, or {@code null} if {@code <R>} is {@link Void}
     */
    @Override
    public Node visitQualifiedStarSelectItem(QualifiedStarSelectItem i) {
        return i;
    }

    /**
     * Visits a base {@link Table} reference.
     *
     * @param t the table being visited
     * @return a result produced by the visitor
     */
    @Override
    public Node visitTable(Table t) {
        return t;
    }

    /**
     * Visits a {@link QueryTable}, representing a derived table
     * or subquery used in the {@code FROM} clause.
     *
     * @param t the query table being visited
     * @return a result produced by the visitor
     */
    @Override
    public Node visitQueryTable(QueryTable t) {
        var query = apply(t.query());
        if (query != t.query()) {
            return QueryTable.of(query);
        }
        return t;
    }

    /**
     * Visits a {@link ValuesTable}, representing an inline {@code VALUES} construct.
     *
     * @param t the values table being visited
     * @return a result produced by the visitor
     */
    @Override
    public Node visitValuesTable(ValuesTable t) {
        var rows = apply(t.rows());
        if (rows != t.rows()) {
            return ValuesTable.of(rows);
        }
        return t;
    }

    /**
     * Visits an {@link OnJoin}, a join with an {@code ON} predicate and a specific join kind
     * (INNER, LEFT, RIGHT, or FULL).
     *
     * @param j the join being visited
     * @return a result produced by the visitor
     */
    @Override
    public Node visitOnJoin(OnJoin j) {
        var table = apply(j.right());
        var predicate = apply(j.on());
        if (table != j.right() || predicate != j.on()) {
            return OnJoin.of(table, j.kind(), predicate);
        }
        return j;
    }

    /**
     * Visits a {@link CrossJoin}, representing a {@code CROSS JOIN} between two sources.
     *
     * @param j the cross join being visited
     * @return a result produced by the visitor
     */
    @Override
    public Node visitCrossJoin(CrossJoin j) {
        var table = apply(j.right());
        if (table != j.right()) {
            return CrossJoin.of(table);
        }
        return j;
    }

    /**
     * Visits a {@link NaturalJoin}, representing a {@code NATURAL JOIN}.
     *
     * @param j the natural join being visited
     * @return a result produced by the visitor
     */
    @Override
    public Node visitNaturalJoin(NaturalJoin j) {
        var table = apply(j.right());
        if (table != j.right()) {
            return CrossJoin.of(table);
        }
        return j;
    }

    /**
     * Visits a {@link UsingJoin}, representing a join with a {@code USING(...)} clause.
     *
     * @param j the using join being visited
     * @return a result produced by the visitor
     */
    @Override
    public Node visitUsingJoin(UsingJoin j) {
        var table = apply(j.right());
        if (table != j.right()) {
            return CrossJoin.of(table);
        }
        return j;
    }

    /**
     * Visits an entire {@link GroupBy} clause.
     * <p>
     * Typical implementations will iterate over the contained
     * {@link GroupItem} elements and may apply dialect-specific
     * normalization or validation.
     * </p>
     *
     * @param g the {@code GROUP BY} node being visited (never {@code null})
     * @return a result value, or {@code null} if {@code <R>} is {@link Void}
     */
    @Override
    public Node visitGroupBy(GroupBy g) {
        List<GroupItem> items = new ArrayList<>();
        if (apply(g.items(), items)) {
            return GroupBy.of(items);
        }
        return g;
    }

    /**
     * Visits a single {@link GroupItem}, representing an individual
     * grouping expression within a {@code GROUP BY} clause.
     *
     * @param i the grouping item being visited (never {@code null})
     * @return a result value, or {@code null} if {@code <R>} is {@link Void}
     */
    @Override
    public Node visitGroupItem(GroupItem i) {
        var expr = apply(i.expr());
        if (expr != i.expr()) {
            return GroupItem.of(expr);
        }
        return i;
    }

    /**
     * Visits an entire {@link OrderBy} clause.
     * <p>
     * Implementations typically traverse its {@link OrderItem} elements
     * and may apply formatting, validation, or ordering transformations.
     * </p>
     *
     * @param o the {@code ORDER BY} node being visited (never {@code null})
     * @return a result value, or {@code null} if {@code <R>} is {@link Void}
     */
    @Override
    public Node visitOrderBy(OrderBy o) {
        List<OrderItem> items = new ArrayList<>();
        if (apply(o.items(), items)) {
            return OrderBy.of(items);
        }
        return o;
    }

    /**
     * Visits a single {@link OrderItem}, representing one
     * ordering expression (expr.g. {@code col DESC NULLS LAST})
     * within an {@code ORDER BY} clause.
     *
     * @param i the order item being visited (never {@code null})
     * @return a result value, or {@code null} if {@code <R>} is {@link Void}
     */
    @Override
    public Node visitOrderItem(OrderItem i) {
        var expr = apply(i.expr());
        if (expr != i.expr()) {
            return OrderItem.of(expr, i.direction(), i.nulls(), i.collate());
        }
        return i;
    }

    /**
     * Visits a {@link LimitOffset} node that represents the
     * pagination segment of a SQL query.
     *
     * @param l the pagination node being visited (never {@code null})
     * @return a result value, or {@code null} if {@code <R>} is {@link Void}
     */
    @Override
    public Node visitLimitOffset(LimitOffset l) {
        return l;
    }

    /**
     * Visits an {@link AnyAllPredicate}, representing
     * {@code <expr> = ANY(<subquery>)} or {@code <expr> > ALL(<subquery>)} constructs.
     *
     * @param p the predicate being visited
     * @return a result produced by the visitor
     */
    @Override
    public Node visitAnyAllPredicate(AnyAllPredicate p) {
        var lhs = apply(p.lhs());
        var subquery = apply(p.subquery());
        if (lhs != p.lhs() || subquery != p.subquery()) {
            return AnyAllPredicate.of(lhs, p.operator(), subquery, p.quantifier());
        }
        return p;
    }

    /**
     * Visits a {@link BetweenPredicate}, representing a {@code BETWEEN} test.
     *
     * @param p the predicate being visited
     * @return a result produced by the visitor
     */
    @Override
    public Node visitBetweenPredicate(BetweenPredicate p) {
        var value = apply(p.value());
        var lower = apply(p.lower());
        var upper = apply(p.upper());
        if (value != p.value() || lower != p.lower() || upper != p.upper()) {
            return BetweenPredicate.of(value, lower, upper, p.symmetric());
        }
        return p;
    }

    /**
     * Visits a {@link ComparisonPredicate}, representing a comparison operator such as
     * {@code =}, {@code <>}, {@code <}, {@code >}, {@code <=}, or {@code >=}.
     *
     * @param p the predicate being visited
     * @return a result produced by the visitor
     */
    @Override
    public Node visitComparisonPredicate(ComparisonPredicate p) {
        var lhs = apply(p.lhs());
        var rhs = apply(p.rhs());
        if (lhs != p.lhs() || rhs != p.rhs()) {
            return ComparisonPredicate.of(lhs, p.operator(), rhs);
        }
        return p;
    }

    /**
     * Visits an {@link ExistsPredicate}, representing an {@code EXISTS(subquery)} test.
     *
     * @param p the predicate being visited
     * @return a result produced by the visitor
     */
    @Override
    public Node visitExistsPredicate(ExistsPredicate p) {
        var subquery = apply(p.subquery());
        if (subquery != p.subquery()) {
            return ExistsPredicate.of(subquery, p.negated());
        }
        return p;
    }

    /**
     * Visits an {@link InPredicate}, representing an {@code IN(...)} test.
     *
     * @param p the predicate being visited
     * @return a result produced by the visitor
     */
    @Override
    public Node visitInPredicate(InPredicate p) {
        var lhs = apply(p.lhs());
        var rhs = apply(p.rhs());
        if (lhs != p.lhs() || rhs != p.rhs()) {
            return InPredicate.of(lhs, rhs, p.negated());
        }
        return p;
    }

    /**
     * Visits an {@link IsNullPredicate}, representing an {@code IS [NOT] NULL} test.
     *
     * @param p the predicate being visited
     * @return a result produced by the visitor
     */
    @Override
    public Node visitIsNullPredicate(IsNullPredicate p) {
        var expr = apply(p.expr());
        if (expr != p.expr()) {
            return IsNullPredicate.of(expr, p.negated());
        }
        return p;
    }

    /**
     * Visits a {@link LikePredicate}, representing a {@code LIKE} or {@code NOT LIKE} pattern match.
     *
     * @param p the predicate being visited
     * @return a result produced by the visitor
     */
    @Override
    public Node visitLikePredicate(LikePredicate p) {
        var value = apply(p.value());
        var pattern = apply(p.pattern());
        var escape = apply(p.escape());
        if (value != p.value() || pattern != p.pattern() || escape != p.escape()) {
            return LikePredicate.of(value, pattern, escape, p.negated());
        }
        return p;
    }

    /**
     * Visits a {@link NotPredicate}, representing logical negation ({@code NOT ...}).
     *
     * @param p the predicate being visited
     * @return a result produced by the visitor
     */
    @Override
    public Node visitNotPredicate(NotPredicate p) {
        var expr = apply(p.inner());
        if (expr != p.inner()) {
            return NotPredicate.of(expr);
        }
        return p;
    }

    /**
     * Visits a {@link UnaryPredicate}, representing a single-operand predicate such as
     * {@code TRUE}, {@code active}, or other unary forms.
     *
     * @param p the predicate being visited
     * @return a result produced by the visitor
     */
    @Override
    public Node visitUnaryPredicate(UnaryPredicate p) {
        var expr = apply(p.expr());
        if (expr != p.expr()) {
            return UnaryPredicate.of(expr);
        }
        return p;
    }

    /**
     * Visits an {@link AndPredicate}, representing a logical conjunction ({@code AND}).
     *
     * @param p the predicate being visited
     * @return a result produced by the visitor
     */
    @Override
    public Node visitAndPredicate(AndPredicate p) {
        var lhs = apply(p.lhs());
        var rhs = apply(p.rhs());
        if (lhs != p.lhs() || rhs != p.rhs()) {
            return AndPredicate.of(lhs, rhs);
        }
        return p;
    }

    /**
     * Visits an {@link OrPredicate}, representing a logical disjunction ({@code OR}).
     *
     * @param p the predicate being visited
     * @return a result produced by the visitor
     */
    @Override
    public Node visitOrPredicate(OrPredicate p) {
        var lhs = apply(p.lhs());
        var rhs = apply(p.rhs());
        if (lhs != p.lhs() || rhs != p.rhs()) {
            return OrPredicate.of(lhs, rhs);
        }
        return p;
    }

    /**
     * Visits a simple {@link SelectQuery}, the basic {@code SELECT ... FROM ...} form.
     *
     * @param q the select query being visited
     * @return a result produced by the visitor
     */
    @Override
    public Node visitSelectQuery(SelectQuery q) {
        List<SelectItem> items = new ArrayList<>();
        boolean changed = apply(q.select(), items);
        var from = apply(q.from());
        changed |= from != q.from();
        List<Join> joins = new ArrayList<>();
        changed |= apply(q.joins(), joins);
        var where = apply(q.where());
        changed |= where != q.where();
        var groupBy = apply(q.groupBy());
        changed |= groupBy != q.groupBy();
        var having = apply(q.having());
        changed |= having != q.having();
        var orderBy = apply(q.orderBy());
        changed |= orderBy != q.orderBy();
        var queryLimitOffset = LimitOffset.of(q.limit(), q.offset());
        var limitOffset = apply(queryLimitOffset);
        changed |= limitOffset != queryLimitOffset;
        if (changed) {
            var query = SelectQuery.of()
                                   .select(items)
                                   .from(from)
                                   .join(joins)
                                   .where(where)
                                   .having(having);

            if (groupBy != null) {
                query.groupBy(groupBy.items());
            }
            if (orderBy != null) {
                query.orderBy(orderBy.items());
            }
            if (limitOffset.limit() != null) {
                q.limit(limitOffset.limit());
            }
            if (limitOffset.offset() != null) {
                q.offset(limitOffset.offset());
            }
            return query;
        }
        return q;
    }

    /**
     * Visits a {@link CompositeQuery}, representing a set operation such as
     * {@code UNION}, {@code INTERSECT}, or {@code EXCEPT}.
     *
     * @param q the composite query being visited
     * @return a result produced by the visitor
     */
    @Override
    public Node visitCompositeQuery(CompositeQuery q) {
        List<Query> terms = new ArrayList<>();
        boolean changed = apply(q.terms(), terms);
        var orderBy = apply(q.orderBy());
        changed |= orderBy != q.orderBy();
        var limitOffset = apply(q.limitOffset());
        changed |= limitOffset != q.limitOffset();
        if (changed) {
            return CompositeQuery.of(terms, q.ops(), orderBy, limitOffset);
        }
        return q;
    }

    /**
     * Visits a {@link WithQuery}, representing a common table expression (CTE)
     * introduced with the {@code WITH} clause.
     *
     * @param q the with query being visited
     * @return a result produced by the visitor
     */
    @Override
    public Node visitWithQuery(WithQuery q) {
        List<CteDef> ctes = new ArrayList<>();
        boolean changed = apply(q.ctes(), ctes);
        var body = apply(q.body());
        changed |= body != q.body();
        if (changed) {
            return WithQuery.of(ctes, body, q.recursive());
        }
        return q;
    }

    @Override
    public Node visitWhenThen(WhenThen w) {
        var when = apply(w.when());
        var then = apply(w.then());
        if (when != w.when() || then != w.then()) {
            return WhenThen.of(when, then);
        }
        return w;
    }

    @Override
    public Node visitCte(CteDef c) {
        var body = apply(c.body());
        if (body != c.body()) {
            return CteDef.of(c.name(), body, c.columnAliases());
        }
        return c;
    }

    @Override
    public Node visitWindowDef(WindowDef w) {
        var spec = apply(w.spec());
        if (spec != w.spec()) {
            return WindowDef.of(w.name(), spec);
        }
        return w;
    }

    @Override
    public Node visitOverRef(OverSpec.Ref r) {
        return r;
    }

    @Override
    public Node visitOverDef(OverSpec.Def d) {
        var partitionBy = apply(d.partitionBy());
        var orderBy = apply(d.orderBy());
        var frame = apply(d.frame());
        if (partitionBy != d.partitionBy() || orderBy != d.orderBy() || frame != d.frame()) {
            if (d.baseWindow() == null) {
                return OverSpec.def(partitionBy, orderBy, frame, d.exclude());
            }
            return OverSpec.def(d.baseWindow(), orderBy, frame, d.exclude());
        }
        return d;
    }

    @Override
    public Node visitPartitionBy(PartitionBy p) {
        List<Expression> items = new ArrayList<>();
        if (apply(p.items(), items)) {
            return PartitionBy.of(items);
        }
        return p;
    }

    @Override
    public Node visitFrameSingle(FrameSpec.Single f) {
        var bound = apply(f.bound());
        if (bound != f.bound()) {
            return FrameSpec.single(f.unit(), bound);
        }
        return f;
    }

    @Override
    public Node visitFrameBetween(FrameSpec.Between f) {
        var start = apply(f.start());
        var end = apply(f.end());
        if (start != f.start() || end != f.end()) {
            return FrameSpec.between(f.unit(), start, end);
        }
        return f;
    }

    // bounds
    @Override
    public Node visitBoundUnboundedPreceding(BoundSpec.UnboundedPreceding b) {
        return b;
    }

    @Override
    public Node visitBoundPreceding(BoundSpec.Preceding b) {
        var expr = apply(b.expr());
        if (expr != b.expr()) {
            return BoundSpec.preceding(expr);
        }
        return b;
    }

    @Override
    public Node visitBoundCurrentRow(BoundSpec.CurrentRow b) {
        return b;
    }

    @Override
    public Node visitBoundFollowing(BoundSpec.Following b) {
        var expr = apply(b.expr());
        if (expr != b.expr()) {
            return BoundSpec.following(expr);
        }
        return b;
    }

    @Override
    public Node visitBoundUnboundedFollowing(BoundSpec.UnboundedFollowing b) {
        return b;
    }
}
