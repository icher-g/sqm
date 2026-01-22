package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.List;

/**
 * Represents a CASE expression.
 * <p>For example:</p>
 * <pre>
 *     {@code
 *     CASE WHEN x = 1 THEN 10 WHEN x = 2 THEN 20 END
 *     }
 * </pre>
 */
public non-sealed interface CaseExpr extends Expression {

    /**
     * Creates an instance of CaseExpr.
     *
     * @param whens an array of WHEN...THEN statements.
     * @return a newly created instance.
     */
    static CaseExpr of(WhenThen... whens) {
        return new Impl(List.of(whens), null);
    }

    /**
     * Creates an instance of CaseExpr.
     *
     * @param whens a list of WHEN...THEN statements.
     * @return a newly created instance.
     */
    static CaseExpr of(List<WhenThen> whens) {
        return new Impl(whens, null);
    }

    /**
     * Creates an instance of CaseExpr.
     *
     * @param whens a list of WHEN...THEN statements.
     * @param elseExpr statement represented by the {@link Expression}.
     * @return a newly created instance.
     */
    static CaseExpr of(List<WhenThen> whens, Expression elseExpr) {
        return new Impl(whens, elseExpr);
    }

    /**
     * Creates a new instance of {@link CaseExpr} preserving current values of {@link CaseExpr#whens}.
     *
     * @param elseExpr statement represented by the {@link Expression}.
     * @return a newly created instance.
     */
    default CaseExpr elseExpr(Expression elseExpr) {
        return new Impl(whens(), elseExpr);
    }

    /**
     * Creates a new instance of {@link CaseExpr} preserving current values of {@link CaseExpr#whens}.
     *
     * @param value a value.
     * @return a newly created instance.
     */
    default CaseExpr elseValue(Object value) {
        return new Impl(whens(), Expression.literal(value));
    }

    /**
     * Gets a WHEN...THEN statement.
     *
     * @return a WHEN...THEN statement.
     */
    List<WhenThen> whens();

    /**
     * Gets an ELSE expression if exists. Can be NULL.
     *
     * @return an ELSE expression if exists or NULL otherwise.
     */
    Expression elseExpr();

    /**
     * Accepts a {@link NodeVisitor} and dispatches control to the
     * visitor method corresponding to the concrete subtype
     *
     * @param v   the visitor instance to accept (must not be {@code null})
     * @param <R> the result type returned by the visitor
     * @return the result produced by the visitor
     */
    @Override
    default <R> R accept(NodeVisitor<R> v) {
        return v.visitCaseExpr(this);
    }

    /**
     * Represents a CASE expression.
     * <p>For example:</p>
     * <pre>
     *     {@code
     *     CASE WHEN x = 1 THEN 10 WHEN x = 2 THEN 20 END AS result
     *     }
     * </pre>
     *
     * @param whens    a list of WHEN...THEN statements.
     * @param elseExpr an ELSE expression.
     */
    record Impl(List<WhenThen> whens, Expression elseExpr) implements CaseExpr {
    }
}
