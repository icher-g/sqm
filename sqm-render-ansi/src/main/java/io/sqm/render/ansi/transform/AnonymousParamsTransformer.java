package io.sqm.render.ansi.transform;

import io.sqm.core.*;
import io.sqm.core.transform.RecursiveNodeTransformer;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Transformer that normalizes all {@link ParamExpr} instances to
 * {@link AnonymousParamExpr} with deterministic 1-based positions.
 * <p>
 * This is primarily useful when preparing a query for ANSI / JDBC style
 * rendering, where only {@code ?} params are supported. The transformer:
 * <ul>
 *   <li>Converts {@link NamedParamExpr} to {@link AnonymousParamExpr},</li>
 *   <li>Converts {@link OrdinalParamExpr} to {@link AnonymousParamExpr},</li>
 *   <li>Reassigns {@link AnonymousParamExpr} positions if necessary.</li>
 * </ul>
 * <p>
 * Positions are assigned in a global, depth-first traversal order based on
 * the first appearance of each parameter:
 * <ul>
 *   <li>Repeated usages of the same {@link NamedParamExpr#name()} share
 *       a single position.</li>
 *   <li>Repeated usages of the same {@link OrdinalParamExpr#index()} share
 *       a single position.</li>
 *   <li>Anonymous params always receive a new position per occurrence.</li>
 * </ul>
 * <p>
 * The transformer also tracks mappings from logical positions back to the
 * original parameter expressions and, for named/ordinal params, to
 * their canonical identifiers. These mappings can be used by a binding
 * layer to assign parameter values in a deterministic way.
 */
public final class AnonymousParamsTransformer extends RecursiveNodeTransformer {

    /**
     * Creates a transformer that normalizes parameters to anonymous placeholders.
     */
    public AnonymousParamsTransformer() {
    }

    /**
     * Mapping from named parameter identifiers to their assigned positions.
     * <p>
     * Example: {@code "tenantId" -> 1}.
     * <p>
     * {@link LinkedHashMap} preserves insertion order so that the first
     * occurrence defines the position.
     */
    private final Map<String, Integer> namedPositions = new LinkedHashMap<>();

    /**
     * Mapping from ordinal parameter indices to their assigned positions.
     * <p>
     * Example: {@code 3 -> 2} meaning {@code $3} (or {@code ?3}) becomes
     * anonymous position 2.
     */
    private final Map<Integer, Integer> ordinalPositions = new LinkedHashMap<>();

    /**
     * Mapping from assigned positions to the original parameter expression
     * that first introduced that position. This can be used by the binding
     * layer to reconstruct the original parameter identity if needed.
     */
    private final Map<Integer, ParamExpr> params = new LinkedHashMap<>();

    /**
     * Next position to assign to a newly encountered parameter.
     * <p>
     * Positions are 1-based.
     */
    private int nextPosition = 1;

    /**
     * Returns an immutable view of the mapping from assigned positions back
     * to the original parameter expressions that introduced them.
     *
     * @return unmodifiable map of {@code position -> original ParamExpr}
     */
    public Map<Integer, ParamExpr> paramsByIndex() {
        return Collections.unmodifiableMap(params);
    }

    /**
     * Allocates a new 1-based position for the given parameter expression
     * and records the original parameter for later inspection.
     *
     * @param p the parameter expression to assign a position to
     * @return the assigned position
     */
    private int allocatePosition(ParamExpr p) {
        int position = nextPosition++;
        params.put(position, p);
        return position;
    }

    /**
     * Visits an {@link AnonymousParamExpr}. Each occurrence of an anonymous
     * parameter receives a new position in the global sequence.
     *
     * @param p the anonymous positional parameter expression
     * @return a new {@link AnonymousParamExpr} with a normalized position
     */
    @Override
    public Node visitAnonymousParamExpr(AnonymousParamExpr p) {
        allocatePosition(p);
        return AnonymousParamExpr.of();
    }

    /**
     * Visits a {@link NamedParamExpr}. All occurrences of a given parameter
     * name share the same position. The first occurrence defines the position,
     * subsequent ones reuse it.
     *
     * @param p the named parameter expression
     * @return an {@link AnonymousParamExpr} with the assigned position
     */
    @Override
    public Node visitNamedParamExpr(NamedParamExpr p) {
        namedPositions.computeIfAbsent(p.name(), name -> allocatePosition(p));
        return AnonymousParamExpr.of();
    }

    /**
     * Visits an {@link OrdinalParamExpr}. All occurrences of a given ordinal
     * index share the same position. The first occurrence defines the position,
     * subsequent ones reuse it.
     *
     * @param p the ordinal parameter expression
     * @return an {@link AnonymousParamExpr} with the assigned position
     */
    @Override
    public Node visitOrdinalParamExpr(OrdinalParamExpr p) {
        ordinalPositions.computeIfAbsent(p.index(), idx -> allocatePosition(p));
        return AnonymousParamExpr.of();
    }
}
