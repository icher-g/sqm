package io.sqm.playground.api;

import java.util.List;

/**
 * Named child slot within a browser-friendly AST node representation.
 *
 * @param slot semantic property name such as {@code selectItems} or {@code where}
 * @param multiple whether the slot represents a collection
 * @param nodes ordered child nodes within the slot
 */
public record AstChildSlotDto(
    String slot,
    boolean multiple,
    List<AstNodeDto> nodes
) {
}
