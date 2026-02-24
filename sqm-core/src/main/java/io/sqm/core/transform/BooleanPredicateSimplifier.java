package io.sqm.core.transform;

import io.sqm.core.*;

/**
 * Canonicalizes boolean predicate trees using safe local rewrites.
 *
 * <p>This transformer applies semantics-preserving simplifications such as:</p>
 * <ul>
 *     <li>{@code NOT (NOT p) -> p}</li>
 *     <li>{@code NOT TRUE -> FALSE}</li>
 *     <li>{@code NOT FALSE -> TRUE}</li>
 *     <li>{@code p AND TRUE -> p}</li>
 *     <li>{@code TRUE AND p -> p}</li>
 *     <li>{@code p AND FALSE -> FALSE}</li>
 *     <li>{@code FALSE AND p -> FALSE}</li>
 *     <li>{@code p OR FALSE -> p}</li>
 *     <li>{@code FALSE OR p -> p}</li>
 *     <li>{@code p OR TRUE -> TRUE}</li>
 *     <li>{@code TRUE OR p -> TRUE}</li>
 * </ul>
 *
 * <p>Only literal boolean predicates are folded. Expressions with SQL NULL are
 * intentionally not simplified here to avoid changing three-valued SQL logic.</p>
 */
public final class BooleanPredicateSimplifier extends RecursiveNodeTransformer {

    /**
     * Creates a boolean predicate simplifier.
     */
    public BooleanPredicateSimplifier() {
    }

    private static Predicate boolPredicate(boolean value) {
        return UnaryPredicate.of(LiteralExpr.of(value));
    }

    private static Boolean asBooleanLiteral(Predicate predicate) {
        if (!(predicate instanceof UnaryPredicate unary)) {
            return null;
        }
        if (!(unary.expr() instanceof LiteralExpr literal)) {
            return null;
        }
        if (!(literal.value() instanceof Boolean b)) {
            return null;
        }
        return b;
    }

    /**
     * Visits a unary predicate and rewrites its inner boolean expression.
     *
     * @param p unary predicate
     * @return simplified predicate or original instance
     */
    @Override
    public Node visitUnaryPredicate(UnaryPredicate p) {
        var expr = apply(p.expr());
        if (expr == p.expr()) {
            return p;
        }
        return UnaryPredicate.of(expr);
    }

    /**
     * Visits a NOT predicate and applies safe boolean simplifications.
     *
     * @param p not predicate
     * @return simplified predicate or original instance
     */
    @Override
    public Node visitNotPredicate(NotPredicate p) {
        var inner = apply(p.inner());

        if (inner instanceof NotPredicate nested) {
            return nested.inner();
        }

        Boolean bool = asBooleanLiteral(inner);
        if (bool != null) {
            return boolPredicate(!bool);
        }

        if (inner == p.inner()) {
            return p;
        }
        return NotPredicate.of(inner);
    }

    /**
     * Visits an AND predicate and applies local boolean simplification rules.
     *
     * @param p and predicate
     * @return simplified predicate or original instance
     */
    @Override
    public Node visitAndPredicate(AndPredicate p) {
        var lhs = apply(p.lhs());
        var rhs = apply(p.rhs());

        Boolean l = asBooleanLiteral(lhs);
        Boolean r = asBooleanLiteral(rhs);

        if (l != null && r != null) {
            return boolPredicate(l && r);
        }
        if (Boolean.TRUE.equals(l)) {
            return rhs;
        }
        if (Boolean.TRUE.equals(r)) {
            return lhs;
        }
        if (Boolean.FALSE.equals(l)) {
            return lhs;
        }
        if (Boolean.FALSE.equals(r)) {
            return rhs;
        }

        if (lhs == p.lhs() && rhs == p.rhs()) {
            return p;
        }
        return AndPredicate.of(lhs, rhs);
    }

    /**
     * Visits an OR predicate and applies local boolean simplification rules.
     *
     * @param p or predicate
     * @return simplified predicate or original instance
     */
    @Override
    public Node visitOrPredicate(OrPredicate p) {
        var lhs = apply(p.lhs());
        var rhs = apply(p.rhs());

        Boolean l = asBooleanLiteral(lhs);
        Boolean r = asBooleanLiteral(rhs);

        if (l != null && r != null) {
            return boolPredicate(l || r);
        }
        if (Boolean.FALSE.equals(l)) {
            return rhs;
        }
        if (Boolean.FALSE.equals(r)) {
            return lhs;
        }
        if (Boolean.TRUE.equals(l)) {
            return lhs;
        }
        if (Boolean.TRUE.equals(r)) {
            return rhs;
        }

        if (lhs == p.lhs() && rhs == p.rhs()) {
            return p;
        }
        return OrPredicate.of(lhs, rhs);
    }
}
