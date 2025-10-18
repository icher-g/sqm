package io.cherlabs.sqm.core;

import java.util.List;

/**
 * Represents a OrderBy statement with the list of Order items.
 * @param items
 */
public record OrderBy(List<Order> items) implements Entity {
}
