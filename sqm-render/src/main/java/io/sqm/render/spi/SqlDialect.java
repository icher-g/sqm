package io.sqm.render.spi;

import io.sqm.core.Node;
import io.sqm.core.OrdinalParamExpr;
import io.sqm.core.collect.ParametersCollector;
import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.core.transform.ParameterizeLiteralsTransformer;

import java.util.List;

/**
 * An interface to hold all dialect related staff.
 */
public interface SqlDialect {
    /**
     * Optionally transforms the query tree before rendering.
     * <p>
     * Implementations may normalize params, rewrite constructs that
     * are not supported by the dialect, or apply other dialect-specific
     * transformations. The default implementation returns the node
     * unchanged.
     *
     * @param root    the root node to be rendered
     * @param options render options (including parameterization mode)
     * @return a node that is safe to render for this dialect
     */
    default PreparedNode beforeRender(Node root, RenderOptions options) {
        if (options.parameterizationMode() == ParameterizationMode.Bind) {
            var collector = new ParametersCollector();
            root.accept(collector);

            if (collector.positional().isEmpty() && collector.named().isEmpty()) {
                // convert all literals to params.
                var literalsTransformer = new ParameterizeLiteralsTransformer((i) -> OrdinalParamExpr.of(i));
                root = root.accept(literalsTransformer);

                return PreparedNode.of(root, literalsTransformer.values());
            }
            throw new IllegalStateException("BIND parameterization mode is not supported for query that already has parameters.");
        }
        return PreparedNode.of(root, List.of());
    }

    /**
     * The name of the dialect. Mostly for the debugging/logging purposes.
     *
     * @return a name of the dialect.
     */
    String name();

    /**
     * Gets an identifier quoter that decides how identifiers are quoted/qualified.
     *
     * @return a quoter.
     */
    IdentifierQuoter quoter();

    /**
     * Gets a value formatter that formats a value into a string.
     *
     * @return a formatter.
     */
    ValueFormatter formatter();

    /**
     * Gets operators that customise tokens for arithmetic/comparison/string ops.
     *
     * @return operators.
     */
    Operators operators();

    /**
     * Gets booleans that define boolean literals and predicate rules.
     *
     * @return booleans.
     */
    Booleans booleans();

    /**
     * Gets null sorting definition that provides null ordering policy and emulation.
     *
     * @return null sorting.
     */
    NullSorting nullSorting();

    /**
     * Gets a pagination style definition.
     *
     * @return a pagination style.
     */
    PaginationStyle paginationStyle();

    /**
     * Returns dialect capabilities used for feature gating during rendering.
     *
     * @return dialect capabilities
     */
    DialectCapabilities capabilities();

    /**
     * Gets renderers repository.
     *
     * @return a repository.
     */
    RenderersRepository renderers();
}
