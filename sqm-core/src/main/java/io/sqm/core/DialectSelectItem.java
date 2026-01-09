package io.sqm.core;

/**
 * Marker interface for dialect-specific {@link SelectItem} nodes.
 * <p>
 * Use for select list items that are not represented by ANSI core
 * {@link SelectItem} subtypes.
 * </p>
 */
public non-sealed interface DialectSelectItem extends SelectItem, DialectNode {
}

