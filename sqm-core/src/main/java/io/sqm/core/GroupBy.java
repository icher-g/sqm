package io.sqm.core;

import java.util.List;

/**
 * Represents a GroupBy statement with the list of Group items.
 * @param items
 */
public record GroupBy(List<Group> items) implements Entity {
}
