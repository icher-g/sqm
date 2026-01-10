package io.sqm.core.walk;

import io.sqm.core.*;

public abstract class RecursiveNodeVisitor<R> implements NodeVisitor<R> {

    protected abstract R defaultResult();

    protected R accept(Node n) {
        return n == null ? null : n.accept(this);
    }

    /**
     * Visits a {@link CaseExpr} node representing a {@code CASE WHEN ... THEN ... END} expression.
     *
     * @param c the case expression
     * @return a result produced by the visitor
     */
    @Override
    public R visitCaseExpr(CaseExpr c) {
        c.whens().forEach(this::accept);
        accept(c.elseExpr());
        return defaultResult();
    }

    /**
     * Visits a {@link ColumnExpr} node referencing a table column.
     *
     * @param c the column expression
     * @return a result produced by the visitor
     */
    @Override
    public R visitColumnExpr(ColumnExpr c) {
        return defaultResult();
    }

    /**
     * Visits a {@link FunctionExpr.Arg} node representing a single argument of a function call.
     *
     * @param a the function argument
     * @return a result produced by the visitor
     */
    @Override
    public R visitFunctionArgExpr(FunctionExpr.Arg a) {
        if (a instanceof FunctionExpr.Arg.ExprArg e) {
            accept(e.expr());
        }
        return defaultResult();
    }

    /**
     * Visits a {@link FunctionExpr} node representing a function call.
     *
     * @param f the function expression
     * @return a result produced by the visitor
     */
    @Override
    public R visitFunctionExpr(FunctionExpr f) {
        f.args().forEach(this::accept);
        accept(f.withinGroup());
        accept(f.filter());
        accept(f.over());
        return defaultResult();
    }

    /**
     * Visits an {@link AnonymousParamExpr}, representing an anonymous positional
     * parameter such as {@code ?}.
     *
     * @param p the anonymous parameter expression
     * @return the result of the visit
     */
    public R visitAnonymousParamExpr(AnonymousParamExpr p) {
        return defaultResult();
    }

    /**
     * Visits a {@link NamedParamExpr}, representing a parameter identified by a
     * canonical name such as {@code :id} or {@code @tenant}.
     *
     * @param p the named parameter expression
     * @return the result of the visit
     */
    public R visitNamedParamExpr(NamedParamExpr p) {
        return defaultResult();
    }

    /**
     * Visits an {@link OrdinalParamExpr}, representing a positional parameter
     * with an explicit index such as {@code $1} or {@code ?2}.
     *
     * @param p the ordinal parameter expression
     * @return the result of the visit
     */
    public R visitOrdinalParamExpr(OrdinalParamExpr p) {
        return defaultResult();
    }

    /**
     * Visits a {@link LiteralExpr} node representing a literal value.
     *
     * @param l the literal expression
     * @return a result produced by the visitor
     */
    @Override
    public R visitLiteralExpr(LiteralExpr l) {
        return defaultResult();
    }

    /**
     * Visits a {@link QueryExpr} node representing a scalar subquery
     * used as an expression.
     *
     * @param v the query expression
     * @return a result produced by the visitor
     */
    @Override
    public R visitQueryExpr(QueryExpr v) {
        accept(v.subquery());
        return defaultResult();
    }

    /**
     * Visits a {@link RowExpr} node representing a row value constructor
     * such as {@code (a, b, c)}.
     *
     * @param v the row expression
     * @return a result produced by the visitor
     */
    @Override
    public R visitRowExpr(RowExpr v) {
        v.items().forEach(this::accept);
        return defaultResult();
    }

    /**
     * Visits a {@link RowListExpr} node representing a list of row expressions,
     * for example {@code ((1,2),(3,4))} used in {@code IN} predicates or value sets.
     *
     * @param v the row list expression
     * @return a result produced by the visitor
     */
    @Override
    public R visitRowListExpr(RowListExpr v) {
        v.rows().forEach(this::accept);
        return defaultResult();
    }

    /**
     * Visits a base {@link Table} reference.
     *
     * @param t the table being visited
     * @return a result produced by the visitor
     */
    @Override
    public R visitTable(Table t) {
        return defaultResult();
    }

    /**
     * Visits a {@link QueryTable}, representing a derived table
     * or subquery used in the {@code FROM} clause.
     *
     * @param t the query table being visited
     * @return a result produced by the visitor
     */
    @Override
    public R visitQueryTable(QueryTable t) {
        accept(t.query());
        return defaultResult();
    }

    /**
     * Visits a {@link ValuesTable}, representing an inline {@code VALUES} construct.
     *
     * @param t the values table being visited
     * @return a result produced by the visitor
     */
    @Override
    public R visitValuesTable(ValuesTable t) {
        t.values().accept(this);
        return defaultResult();
    }

    /**
     * Visits an {@link OnJoin}, a join with an {@code ON} predicate and a specific join kind
     * (INNER, LEFT, RIGHT, or FULL).
     *
     * @param j the join being visited
     * @return a result produced by the visitor
     */
    @Override
    public R visitOnJoin(OnJoin j) {
        accept(j.right());
        accept(j.on());
        return defaultResult();
    }

    /**
     * Visits a {@link CrossJoin}, representing a {@code CROSS JOIN} between two sources.
     *
     * @param j the cross join being visited
     * @return a result produced by the visitor
     */
    @Override
    public R visitCrossJoin(CrossJoin j) {
        accept(j.right());
        return defaultResult();
    }

    /**
     * Visits a {@link NaturalJoin}, representing a {@code NATURAL JOIN}.
     *
     * @param j the natural join being visited
     * @return a result produced by the visitor
     */
    @Override
    public R visitNaturalJoin(NaturalJoin j) {
        accept(j.right());
        return defaultResult();
    }

    /**
     * Visits a {@link UsingJoin}, representing a join with a {@code USING(...)} clause.
     *
     * @param j the using join being visited
     * @return a result produced by the visitor
     */
    @Override
    public R visitUsingJoin(UsingJoin j) {
        accept(j.right());
        return defaultResult();
    }

    /**
     * Visits an {@link AnyAllPredicate}, representing
     * {@code <expr> = ANY(<subquery>)} or {@code <expr> > ALL(<subquery>)} constructs.
     *
     * @param p the predicate being visited
     * @return a result produced by the visitor
     */
    @Override
    public R visitAnyAllPredicate(AnyAllPredicate p) {
        accept(p.lhs());
        accept(p.subquery());
        return defaultResult();
    }

    /**
     * Visits a {@link BetweenPredicate}, representing a {@code BETWEEN} test.
     *
     * @param p the predicate being visited
     * @return a result produced by the visitor
     */
    @Override
    public R visitBetweenPredicate(BetweenPredicate p) {
        accept(p.value());
        accept(p.lower());
        accept(p.upper());
        return defaultResult();
    }

    /**
     * Visits a {@link ComparisonPredicate}, representing a comparison operator such as
     * {@code =}, {@code <>}, {@code <}, {@code >}, {@code <=}, or {@code >=}.
     *
     * @param p the predicate being visited
     * @return a result produced by the visitor
     */
    @Override
    public R visitComparisonPredicate(ComparisonPredicate p) {
        accept(p.lhs());
        accept(p.rhs());
        return defaultResult();
    }

    /**
     * Visits an {@link ExistsPredicate}, representing an {@code EXISTS(subquery)} test.
     *
     * @param p the predicate being visited
     * @return a result produced by the visitor
     */
    @Override
    public R visitExistsPredicate(ExistsPredicate p) {
        accept(p.subquery());
        return defaultResult();
    }

    /**
     * Visits an {@link IsNullPredicate}, representing an {@code IS [NOT] NULL} test.
     *
     * @param p the predicate being visited
     * @return a result produced by the visitor
     */
    @Override
    public R visitIsNullPredicate(IsNullPredicate p) {
        accept(p.expr());
        return defaultResult();
    }

    /**
     * Visits a {@link LikePredicate}, representing a {@code LIKE} or {@code NOT LIKE} pattern match.
     *
     * @param p the predicate being visited
     * @return a result produced by the visitor
     */
    @Override
    public R visitLikePredicate(LikePredicate p) {
        accept(p.value());
        accept(p.pattern());
        accept(p.escape());
        return defaultResult();
    }

    /**
     * Visits a {@link NotPredicate}, representing logical negation ({@code NOT ...}).
     *
     * @param p the predicate being visited
     * @return a result produced by the visitor
     */
    @Override
    public R visitNotPredicate(NotPredicate p) {
        accept(p.inner());
        return defaultResult();
    }

    /**
     * Visits a {@link UnaryPredicate}, representing a single-operand predicate such as
     * {@code EXISTS}, {@code IS NULL}, or other unary forms.
     *
     * @param p the predicate being visited
     * @return a result produced by the visitor
     */
    @Override
    public R visitUnaryPredicate(UnaryPredicate p) {
        accept(p.expr());
        return defaultResult();
    }

    /**
     * Visits an {@link InPredicate}, representing an {@code IN(...)} test.
     *
     * @param p the predicate being visited
     * @return a result produced by the visitor
     */
    @Override
    public R visitInPredicate(InPredicate p) {
        accept(p.lhs());
        accept(p.rhs());
        return defaultResult();
    }

    /**
     * Visits an {@link AndPredicate}, representing a logical conjunction ({@code AND}).
     *
     * @param p the predicate being visited
     * @return a result produced by the visitor
     */
    @Override
    public R visitAndPredicate(AndPredicate p) {
        accept(p.lhs());
        accept(p.rhs());
        return defaultResult();
    }

    /**
     * Visits an {@link OrPredicate}, representing a logical disjunction ({@code OR}).
     *
     * @param p the predicate being visited
     * @return a result produced by the visitor
     */
    @Override
    public R visitOrPredicate(OrPredicate p) {
        accept(p.lhs());
        accept(p.rhs());
        return defaultResult();
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
    public R visitExprSelectItem(ExprSelectItem i) {
        accept(i.expr());
        return defaultResult();
    }

    /**
     * Visits a {@link StarSelectItem}, representing an unqualified
     * {@code *} projection.
     * Example: {@code SELECT * FROM users}
     *
     * @param i the star select item being visited (never {@code null})
     * @return a result value, or {@code null} if {@code <R>} is {@link Void}
     */
    @Override
    public R visitStarSelectItem(StarSelectItem i) {
        return defaultResult();
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
    public R visitQualifiedStarSelectItem(QualifiedStarSelectItem i) {
        return defaultResult();
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
    public R visitGroupBy(GroupBy g) {
        g.items().forEach(this::accept);
        return defaultResult();
    }

    /**
     * Visits a single {@link GroupItem}, representing an individual
     * grouping expression within a {@code GROUP BY} clause.
     *
     * @param i the grouping item being visited (never {@code null})
     * @return a result value, or {@code null} if {@code <R>} is {@link Void}
     */
    @Override
    public R visitGroupItem(GroupItem i) {
        accept(i.expr());
        return defaultResult();
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
    public R visitOrderBy(OrderBy o) {
        o.items().forEach(this::accept);
        return defaultResult();
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
    public R visitOrderItem(OrderItem i) {
        accept(i.expr());
        return defaultResult();
    }

    /**
     * Visits a {@link LimitOffset} node that represents the
     * pagination segment of a SQL query.
     *
     * @param l the pagination node being visited (never {@code null})
     * @return a result value, or {@code null} if {@code <R>} is {@link Void}
     */
    @Override
    public R visitLimitOffset(LimitOffset l) {
        return defaultResult();
    }

    /**
     * Visits a {@link CompositeQuery}, representing a set operation such as
     * {@code UNION}, {@code INTERSECT}, or {@code EXCEPT}.
     *
     * @param q the composite query being visited
     * @return a result produced by the visitor
     */
    @Override
    public R visitCompositeQuery(CompositeQuery q) {
        q.terms().forEach(this::accept);
        accept(q.orderBy());
        accept(q.limitOffset());
        return defaultResult();
    }

    /**
     * Visits a {@link WithQuery}, representing a common table expression (CTE)
     * introduced with the {@code WITH} clause.
     *
     * @param q the with query being visited
     * @return a result produced by the visitor
     */
    @Override
    public R visitWithQuery(WithQuery q) {
        q.ctes().forEach(this::accept);
        accept(q.body());
        return defaultResult();
    }

    /**
     * Visits a simple {@link SelectQuery}, the basic {@code SELECT ... FROM ...} form.
     *
     * @param q the select query being visited
     * @return a result produced by the visitor
     */
    @Override
    public R visitSelectQuery(SelectQuery q) {
        q.items().forEach(this::accept);
        accept(q.distinct());
        accept(q.from());
        q.joins().forEach(this::accept);
        accept(q.where());
        accept(q.groupBy());
        accept(q.having());
        q.windows().forEach(this::accept);
        accept(q.orderBy());
        if (q.limit() != null || q.offset() != null) {
            LimitOffset.of(q.limit(), q.offset()).accept(this);
        }
        return defaultResult();
    }

    /**
     * Visits a single {@link WhenThen} clause within a {@link io.sqm.core.Expression}
     * such as a {@code CASE WHEN ... THEN ...} construct.
     *
     * @param w the {@link WhenThen} clause being visited
     * @return a result specific to the visitor implementation
     */
    @Override
    public R visitWhenThen(WhenThen w) {
        accept(w.when());
        accept(w.then());
        return defaultResult();
    }

    /**
     * Visits a {@link CteDef} (Common Table Expression definition) node used in
     * {@code WITH} queries to define reusable subqueries.
     *
     * @param c the CTE definition being visited
     * @return a result specific to the visitor implementation
     */
    @Override
    public R visitCte(CteDef c) {
        accept(c.body());
        return defaultResult();
    }

    /**
     * Visits a {@link WindowDef} node representing a named window defined in the
     * {@code WINDOW} clause of a {@code SELECT} statement.
     * <p>Example SQL:</p>
     * <pre>
     * SELECT RANK() OVER w FROM employees
     * WINDOW w AS (PARTITION BY dept ORDER BY salary DESC)
     * </pre>
     *
     * @param w the window definition
     * @return a result of type {@code R}
     */
    @Override
    public R visitWindowDef(WindowDef w) {
        // WINDOW w AS ( <OverSpec.Def> )
        accept(w.spec());                  // dive into OverSpec.Def
        return defaultResult();
    }

    /**
     * Visits an {@link OverSpec.Ref} node representing a reference to a named window,
     * as in {@code OVER w}.
     * <p>Example SQL:</p>
     * <pre>
     * RANK() OVER w
     * </pre>
     *
     * @param r the {@code OVER} reference
     * @return a result of type {@code R}
     */
    @Override
    public R visitOverRef(OverSpec.Ref r) {
        return defaultResult();
    }

    /**
     * Visits an {@link OverSpec.Def} node representing an inline {@code OVER(...)}
     * specification attached to a window function.
     * <p>Example SQL:</p>
     * <pre>
     * SUM(amount) OVER (PARTITION BY dept ORDER BY ts)
     * </pre>
     *
     * @param d the {@code OVER} specification
     * @return a result of type {@code R}
     */
    @Override
    public R visitOverDef(OverSpec.Def d) {
        accept(d.partitionBy());
        accept(d.orderBy());
        accept(d.frame());
        return defaultResult();
    }

    /**
     * Visits an {@link PartitionBy} node representing a {@code PARTITION BY}
     * specification attached to a window function.
     * <p>Example SQL:</p>
     * <pre>
     * SUM(amount) OVER (PARTITION BY dept ORDER BY ts)
     * </pre>
     *
     * @param p the {@code PARTITION BY} specification
     * @return a result of type {@code R}
     */
    @Override
    public R visitPartitionBy(PartitionBy p) {
        p.items().forEach(this::accept);
        return defaultResult();
    }

    /**
     * Visits a {@link FrameSpec.Single} node representing a single-bound
     * window frame, such as {@code ROWS 5 PRECEDING} or {@code RANGE UNBOUNDED PRECEDING}.
     *
     * @param f the single-bound frame specification
     * @return a result of type {@code R}
     */
    @Override
    public R visitFrameSingle(FrameSpec.Single f) {
        accept(f.bound());
        return defaultResult();
    }

    /**
     * Visits a {@link FrameSpec.Between} node representing a bounded window
     * frame defined with {@code BETWEEN ... AND ...}, such as:
     * {@code ROWS BETWEEN 2 PRECEDING AND CURRENT ROW}.
     *
     * @param f the between-bound frame specification
     * @return a result of type {@code R}
     */
    @Override
    public R visitFrameBetween(FrameSpec.Between f) {
        accept(f.start());
        accept(f.end());
        return defaultResult();
    }

    /**
     * Visits a {@link BoundSpec.UnboundedPreceding} node representing the
     * {@code UNBOUNDED PRECEDING} frame boundary.
     *
     * @param b the frame boundary
     * @return a result of type {@code R}
     */
    @Override
    public R visitBoundUnboundedPreceding(BoundSpec.UnboundedPreceding b) {
        return defaultResult();
    }

    /**
     * Visits a {@link BoundSpec.Preceding} node representing a frame boundary
     * defined as {@code n PRECEDING}, where {@code n} is typically a numeric expression.
     *
     * @param b the frame boundary
     * @return a result of type {@code R}
     */
    @Override
    public R visitBoundPreceding(BoundSpec.Preceding b) {
        accept(b.expr());
        return defaultResult();
    }

    /**
     * Visits a {@link BoundSpec.CurrentRow} node representing the
     * {@code CURRENT ROW} frame boundary.
     *
     * @param b the frame boundary
     * @return a result of type {@code R}
     */
    @Override
    public R visitBoundCurrentRow(BoundSpec.CurrentRow b) {
        return defaultResult();
    }

    /**
     * Visits a {@link BoundSpec.Following} node representing a frame boundary
     * defined as {@code n FOLLOWING}, where {@code n} is typically a numeric expression.
     *
     * @param b the frame boundary
     * @return a result of type {@code R}
     */
    @Override
    public R visitBoundFollowing(BoundSpec.Following b) {
        accept(b.expr());
        return defaultResult();
    }

    /**
     * Visits a {@link BoundSpec.UnboundedFollowing} node representing the
     * {@code UNBOUNDED FOLLOWING} frame boundary.
     *
     * @param b the frame boundary
     * @return a result of type {@code R}
     */
    @Override
    public R visitBoundUnboundedFollowing(BoundSpec.UnboundedFollowing b) {
        return defaultResult();
    }

    /**
     * Recursively visits an {@link AddArithmeticExpr} node by first visiting
     * its left-hand side and right-hand side operands, and then returning the
     * default visitor result.
     *
     * <p>This method provides a standard depth-first traversal for addition
     * expressions. Subclasses may override this method to implement specialized
     * behavior but should generally invoke {@code accept()} on the operands to
     * ensure full traversal.</p>
     *
     * @param expr the addition expression to visit, never {@code null}
     * @return the value returned by {@link #defaultResult()}
     */
    @Override
    public R visitAddArithmeticExpr(AddArithmeticExpr expr) {
        accept(expr.lhs());
        accept(expr.rhs());
        return defaultResult();
    }

    /**
     * Recursively visits a {@link SubArithmeticExpr} node by first visiting
     * its left-hand side and right-hand side operands, and then returning the
     * default visitor result.
     *
     * @param expr the subtraction expression to visit, never {@code null}
     * @return the value returned by {@link #defaultResult()}
     */
    @Override
    public R visitSubArithmeticExpr(SubArithmeticExpr expr) {
        accept(expr.lhs());
        accept(expr.rhs());
        return defaultResult();
    }

    /**
     * Recursively visits a {@link MulArithmeticExpr} node by first visiting
     * its left-hand side and right-hand side operands, and then returning the
     * default visitor result.
     *
     * @param expr the multiplication expression to visit, never {@code null}
     * @return the value returned by {@link #defaultResult()}
     */
    @Override
    public R visitMulArithmeticExpr(MulArithmeticExpr expr) {
        accept(expr.lhs());
        accept(expr.rhs());
        return defaultResult();
    }

    /**
     * Recursively visits a {@link DivArithmeticExpr} node by first visiting
     * its left-hand side and right-hand side operands, and then returning the
     * default visitor result.
     *
     * @param expr the division expression to visit, never {@code null}
     * @return the value returned by {@link #defaultResult()}
     */
    @Override
    public R visitDivArithmeticExpr(DivArithmeticExpr expr) {
        accept(expr.lhs());
        accept(expr.rhs());
        return defaultResult();
    }

    /**
     * Recursively visits a {@link ModArithmeticExpr} node by first visiting
     * its left-hand side and right-hand side operands, and then returning the
     * default visitor result.
     *
     * @param expr the modulo expression to visit, never {@code null}
     * @return the value returned by {@link #defaultResult()}
     */
    @Override
    public R visitModArithmeticExpr(ModArithmeticExpr expr) {
        accept(expr.lhs());
        accept(expr.rhs());
        return defaultResult();
    }

    /**
     * Recursively visits a {@link NegativeArithmeticExpr} node by first visiting
     * the negated operand, and then returning the default visitor result.
     *
     * @param expr the negation expression to visit, never {@code null}
     * @return the value returned by {@link #defaultResult()}
     */
    @Override
    public R visitNegativeArithmeticExpr(NegativeArithmeticExpr expr) {
        accept(expr.expr());
        return defaultResult();
    }

    /**
     * Visits a {@link DistinctSpec} instance.
     *
     * @param spec DISTINCT specification to visit
     * @return visitor-specific result
     */
    @Override
    public R visitDistinctSpec(DistinctSpec spec) {
        return defaultResult();
    }
}
