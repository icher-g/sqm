package io.sqm.parser.oracle;

import io.sqm.core.Identifier;
import io.sqm.core.ModelColumn;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Immutable declared measure names for one MODEL rule block.
 */
record ModelParseScope(Set<String> measures) {
    ModelParseScope {
        measures = Set.copyOf(measures);
    }

    /**
     * Captures names from MEASURES (amount AS sales, tax).
     */
    static ModelParseScope of(List<ModelColumn> measures) {
        return new ModelParseScope(measures.stream().map(column -> key(column.name())).collect(Collectors.toSet()));
    }

    private static String key(Identifier name) {
        return name.quoted() ? name.value() : name.value().toUpperCase(Locale.ROOT);
    }

    /**
     * Tests a measure prefix such as sales in sales[2025].
     */
    boolean contains(Identifier measure) {
        return measures.contains(key(measure));
    }
}
