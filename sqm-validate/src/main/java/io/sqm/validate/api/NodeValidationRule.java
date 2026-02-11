package io.sqm.validate.api;

import io.sqm.core.Node;

/**
 * Validation rule bound to a specific node type.
 *
 * @param <N> node type accepted by this rule.
 * @param <C> validation context type.
 */
public interface NodeValidationRule<N extends Node, C> {
    /**
     * Node type this rule supports.
     *
     * @return node class token.
     */
    Class<N> nodeType();

    /**
     * Validates node using supplied context.
     *
     * @param node node to validate.
     * @param context validation context.
     */
    void validate(N node, C context);
}

