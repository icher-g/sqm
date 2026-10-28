package io.sqm.parser.oracle;

import io.sqm.core.Identifier;
import io.sqm.core.ModelColumn;
import io.sqm.core.ReferenceModel;

import java.util.List;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Immutable quote-aware main/reference names and declarations for one MODEL rule block.
 */
record ModelParseScope(Identifier mainName, Set<String> dimensions, Set<String> measures, Map<String, ReferenceModel> references) {
    ModelParseScope {
        dimensions = Set.copyOf(dimensions);
        measures = Set.copyOf(measures);
        references = Map.copyOf(references);
    }

    /**
     * Captures names from MEASURES (amount AS sales, tax).
     */
    static ModelParseScope of(List<ModelColumn> measures) {
        return of(null, List.of(), measures, List.of());
    }

    /** Captures MAIN forecast, its declarations, and preceding REFERENCE models. */
    static ModelParseScope of(Identifier name, List<ModelColumn> dimensions, List<ModelColumn> measures, List<ReferenceModel> references) {
        var names = new HashMap<String, ReferenceModel>();
        for (var reference : references) {
            if (names.put(key(reference.name()), reference) != null) {
                throw new IllegalArgumentException("Duplicate MODEL reference name: " + reference.name().value());
            }
        }
        if (name != null && names.containsKey(key(name))) {
            throw new IllegalArgumentException("MAIN name conflicts with a MODEL reference: " + name.value());
        }
        return new ModelParseScope(name, names(dimensions), names(measures), names);
    }

    private static Set<String> names(List<ModelColumn> columns) {
        return columns.stream().map(column -> key(column.name())).collect(Collectors.toSet());
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

    /** Tests forecast.sales[2026] or rates.rate['USD'] in the declared namespace. */
    boolean contains(Identifier model, Identifier measure) {
        if (model == null || mainName != null && key(mainName).equals(key(model))) return contains(measure);
        var reference = references.get(key(model));
        return reference != null && reference.measures().stream().anyMatch(column -> key(column.name()).equals(key(measure)));
    }

    /** Tests a declared dimension used by FOR year_key or CV(year_key). */
    boolean containsDimension(Identifier dimension) {
        return dimensions.contains(key(dimension));
    }

    /** Returns the coordinate count for an unqualified, MAIN-qualified, or reference-qualified cell. */
    int dimensionCount(Identifier model) {
        if (model == null || mainName != null && key(mainName).equals(key(model))) return dimensions.size();
        return references.get(key(model)).dimensions().size();
    }
}
