package io.sqm.validate.schema.internal;

import io.sqm.catalog.model.CatalogColumn;
import io.sqm.core.Identifier;
import io.sqm.core.ModelClause;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Immutable declaration/type snapshot shared by MODEL rule validation and output
 * resolution after input-row expressions have been analyzed.
 *
 * @param clause     source MODEL clause
 * @param columns    main-model columns keyed with quote-aware SQL identifier equality
 * @param references reference-model column maps under the same equality rules
 */
public record ModelScope(ModelClause clause, Map<String, CatalogColumn> columns, Map<String, Map<String, CatalogColumn>> references) {
    /**
     * Copies all model namespaces defensively.
     *
     * @param clause     source MODEL clause
     * @param columns    main-model column metadata
     * @param references reference-model column metadata
     */
    public ModelScope {
        columns = Collections.unmodifiableMap(new LinkedHashMap<>(columns));
        var copy = new LinkedHashMap<String, Map<String, CatalogColumn>>();
        references.forEach((name, fields) -> copy.put(name, Collections.unmodifiableMap(new LinkedHashMap<>(fields))));
        references = Collections.unmodifiableMap(copy);
    }

    /**
     * Returns a SQL identifier key, preserving quoted case and folding unquoted names.
     *
     * @param name identifier
     * @return namespace key
     */
    public static String key(Identifier name) {
        return name.quoted() ? name.value() : name.value().toUpperCase(Locale.ROOT);
    }

    /**
     * Resolves a main/reference model column without falling back to input rows.
     *
     * @param model  optional main/reference qualifier
     * @param column field name
     * @return metadata, or null when the model or field is unknown
     */
    public CatalogColumn resolve(Identifier model, Identifier column) {
        if (model == null || clause.main().name() != null && key(clause.main().name()).equals(key(model))) {
            return columns.get(key(column));
        }
        var fields = references.get(key(model));
        return fields == null ? null : fields.get(key(column));
    }
}
