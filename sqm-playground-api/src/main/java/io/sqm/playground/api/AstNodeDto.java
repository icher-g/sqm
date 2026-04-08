package io.sqm.playground.api;

import java.util.List;

/**
 * Browser-friendly AST node derived from an SQM node.
 *
 * @param nodeType simple Java type name
 * @param nodeInterface fully qualified top-level SQM interface name
 * @param kind JSON discriminator-aligned kind when available
 * @param category coarse UI category such as {@code statement} or {@code expression}
 * @param label short display label
 * @param details scalar metadata entries
 * @param children named child slots
 */
public record AstNodeDto(
    String nodeType,
    String nodeInterface,
    String kind,
    String category,
    String label,
    List<AstDetailDto> details,
    List<AstChildSlotDto> children
) {
}
