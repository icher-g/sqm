package io.sqm.core.collect;

import io.sqm.core.AnonymousParamExpr;
import io.sqm.core.NamedParamExpr;
import io.sqm.core.OrdinalParamExpr;
import io.sqm.core.ParamExpr;
import io.sqm.core.walk.RecursiveNodeVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link RecursiveNodeVisitor} implementation that collects all parameter
 * expressions found in a query AST.
 * <p>
 * SQM distinguishes three kinds of parameters:
 * <ul>
 *     <li>{@link OrdinalParamExpr} – parameters that originate from ordinal
 *         placeholders such as {@code $1}, {@code $2}, ... in PostgreSQL-style SQL.</li>
 *     <li>{@link AnonymousParamExpr} – anonymous placeholders such as {@code ?}
 *         (e.g., JDBC positional parameters).</li>
 *     <li>{@link NamedParamExpr} – named placeholders such as {@code :name} or
 *         {@code @id}, depending on lexer rules and dialect.</li>
 * </ul>
 * <p>
 * This collector walks the entire AST and aggregates them into two lists:
 * <ul>
 *     <li>{@code positional} — contains both ordinal and anonymous parameters,
 *         because both participate in positional ordering when renumbered or
 *         when rendered to JDBC-style {@code ?} placeholders.</li>
 *     <li>{@code named} — contains only named parameters.</li>
 * </ul>
 * <p>
 * The order in which parameters are collected corresponds to their
 * left-to-right order in the logical AST traversal, which is typically what
 * consumers need when constructing a final ordered parameter binding list.
 */
public class ParametersCollector extends RecursiveNodeVisitor<Void> {

    /**
     * Collected positional parameters, including ordinal and anonymous ones.
     */
    private final List<ParamExpr> positional = new ArrayList<>();

    /**
     * Collected named parameters.
     */
    private final List<ParamExpr> named = new ArrayList<>();

    /**
     * Default result for visitor methods that return {@link Void}.
     *
     * @return always {@code null}
     */
    @Override
    protected Void defaultResult() {
        return null;
    }

    /**
     * Returns all positional parameters discovered in the AST.
     * <p>
     * This list includes:
     * <ul>
     *     <li>{@link OrdinalParamExpr} (e.g. {@code $1})</li>
     *     <li>{@link AnonymousParamExpr} (e.g. {@code ?})</li>
     * </ul>
     *
     * @return the list of positional parameters in visitation order
     */
    public List<ParamExpr> positional() {
        return positional;
    }

    /**
     * Returns all named parameters discovered in the AST.
     * <p>
     * This list includes only {@link NamedParamExpr} instances.
     *
     * @return the list of named parameters in visitation order
     */
    public List<ParamExpr> named() {
        return named;
    }

    /**
     * Visits an ordinal parameter expression such as {@code $1} or {@code $2}.
     * Adds the parameter to the {@code positional} list.
     *
     * @param p the ordinal parameter expression
     * @return the result of the recursive visit
     */
    @Override
    public Void visitOrdinalParamExpr(OrdinalParamExpr p) {
        positional.add(p);
        return super.visitOrdinalParamExpr(p);
    }

    /**
     * Visits an anonymous parameter expression such as {@code ?}.
     * Adds the parameter to the {@code positional} list.
     *
     * @param p the anonymous parameter expression
     * @return the result of the recursive visit
     */
    @Override
    public Void visitAnonymousParamExpr(AnonymousParamExpr p) {
        positional.add(p);
        return super.visitAnonymousParamExpr(p);
    }

    /**
     * Visits a named parameter expression such as {@code :name} or {@code @id}.
     * Adds the parameter to the {@code named} list.
     *
     * @param p the named parameter expression
     * @return the result of the recursive visit
     */
    @Override
    public Void visitNamedParamExpr(NamedParamExpr p) {
        named.add(p);
        return super.visitNamedParamExpr(p);
    }
}

