package io.sqm.core.transform;

import io.sqm.core.Node;
import io.sqm.core.OrdinalParamExpr;
import io.sqm.core.ParamExpr;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Task-oriented helpers for literal parameterization and normalization.
 * <p>
 * These utilities are intended for runtime scenarios where a statement or expression
 * is authored with inline literals during development and later needs to be:
 * <ul>
 *     <li>parameterized for bind execution, or</li>
 *     <li>normalized so queries that differ only by literal values share the same shape.</li>
 * </ul>
 */
public final class LiteralTransforms {
    private LiteralTransforms() {
    }

    /**
     * Parameterizes literals using ordinal parameters such as {@code $1}, {@code $2}, ...
     * and returns both the rewritten node and collected bind values.
     * <p>
     * Example:
     * <pre>{@code
     * var parameterized = LiteralTransforms.parameterize(query);
     * var rewritten = parameterized.node();
     * var values = parameterized.values();
     * }</pre>
     *
     * @param node root node to parameterize
     * @param <T>  node type
     * @return rewritten node and collected bind values
     */
    public static <T extends Node> Parameterized<T> parameterize(T node) {
        return parameterize(node, OrdinalParamExpr::of);
    }

    /**
     * Parameterizes literals using the provided parameter factory and returns both
     * the rewritten node and collected bind values.
     * <p>
     * Example:
     * <pre>{@code
     * var parameterized = LiteralTransforms.parameterize(
     *     query,
     *     i -> ParamExpr.named("p" + i)
     * );
     * }</pre>
     *
     * @param node         root node to parameterize
     * @param paramCreator factory for generated parameter nodes
     * @param <T>          node type
     * @return rewritten node and collected bind values
     */
    public static <T extends Node> Parameterized<T> parameterize(T node, Function<Integer, ParamExpr> paramCreator) {
        Objects.requireNonNull(node, "node");
        Objects.requireNonNull(paramCreator, "paramCreator");
        var transformer = new ParameterizeLiteralsTransformer(paramCreator);
        T rewritten = transformer.apply(node);
        return new Parameterized<>(rewritten, transformer.valuesByParam(), transformer.values());
    }

    /**
     * Normalizes a node by replacing all inline literals with ordinal parameters.
     * <p>
     * This is useful for shape-based comparisons such as query fingerprinting.
     *
     * @param node root node to normalize
     * @param <T>  node type
     * @return normalized node
     */
    public static <T extends Node> T normalizeLiterals(T node) {
        return parameterize(node).node();
    }

    /**
     * Result of literal parameterization.
     *
     * @param node          rewritten node with parameter expressions instead of literals
     * @param valuesByParam generated parameters mapped to the literal values they replaced
     * @param values        literal values in encounter order
     * @param <T>           node type
     */
    public record Parameterized<T extends Node>(
        T node,
        Map<ParamExpr, Object> valuesByParam,
        List<Object> values
    ) {
        /**
         * Creates a parameterization result.
         *
         * @param node          rewritten node with parameter expressions instead of literals
         * @param valuesByParam generated parameters mapped to the literal values they replaced
         * @param values        literal values in encounter order
         */
        public Parameterized {
            Objects.requireNonNull(node, "node");
            valuesByParam = Map.copyOf(Objects.requireNonNull(valuesByParam, "valuesByParam"));
            values = List.copyOf(Objects.requireNonNull(values, "values"));
        }
    }
}