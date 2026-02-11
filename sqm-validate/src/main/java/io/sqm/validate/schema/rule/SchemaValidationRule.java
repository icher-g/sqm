package io.sqm.validate.schema.rule;

import io.sqm.core.Node;
import io.sqm.validate.api.NodeValidationRule;
import io.sqm.validate.schema.internal.SchemaValidationContext;

/**
 * Marker contract for schema-specific node validation rules.
 *
 * @param <N> supported node type.
 */
public interface SchemaValidationRule<N extends Node> extends NodeValidationRule<N, SchemaValidationContext> {
}
