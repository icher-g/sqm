package io.sqm.render.spi;

import io.sqm.core.Node;

import java.util.List;

/**
 * A wrapper that holds a transformed {@link Node} together with the parameters
 * that should be applied to it prior to rendering.
 *
 * <p>This type is typically produced by {@link SqlDialect} implementations
 * during the dialect-specific preparation phase. The dialect may rewrite the
 * query tree (e.g., normalize parameters or expand dialect-specific constructs)
 * and at the same time produce a corresponding parameters' collection.</p>
 *
 * @param node   the transformed query tree ready for rendering
 * @param params the complete set of positional and/or named parameter values
 */
public record PreparedNode(Node node, List<Object> params) {

    /**
     * Creates a {@code PreparedNode} with no associated parameters.
     *
     * @param node the prepared node
     * @return a prepared node with an empty parameter list.
     */
    public static PreparedNode of(Node node) {
        return new PreparedNode(node, List.of());
    }

    /**
     * Creates a {@code PreparedNode} with explicitly provided parameters.
     *
     * @param node   the prepared node
     * @param params the parameters associated with the node
     * @return a prepared node with associated parameters.
     */
    public static PreparedNode of(Node node, List<Object> params) {
        return new PreparedNode(node, params);
    }
}

