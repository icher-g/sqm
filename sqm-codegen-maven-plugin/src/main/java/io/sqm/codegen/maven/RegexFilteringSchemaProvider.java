package io.sqm.codegen.maven;

import io.sqm.catalog.SchemaProvider;
import io.sqm.catalog.model.CatalogSchema;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Applies include/exclude regex filters over schema/table names
 * on top of another {@link SchemaProvider}.
 */
final class RegexFilteringSchemaProvider implements SchemaProvider {
    private final SchemaProvider delegate;
    private final List<Pattern> schemaIncludes;
    private final List<Pattern> schemaExcludes;
    private final List<Pattern> tableIncludes;
    private final List<Pattern> tableExcludes;

    /**
     * Creates filtering wrapper.
     *
     * @param delegate underlying schema provider.
     * @param schemaIncludes schema include patterns.
     * @param schemaExcludes schema exclude patterns.
     * @param tableIncludes table include patterns.
     * @param tableExcludes table exclude patterns.
     */
    RegexFilteringSchemaProvider(
        SchemaProvider delegate,
        List<Pattern> schemaIncludes,
        List<Pattern> schemaExcludes,
        List<Pattern> tableIncludes,
        List<Pattern> tableExcludes
    ) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.schemaIncludes = List.copyOf(schemaIncludes);
        this.schemaExcludes = List.copyOf(schemaExcludes);
        this.tableIncludes = List.copyOf(tableIncludes);
        this.tableExcludes = List.copyOf(tableExcludes);
    }

    @Override
    public CatalogSchema load() throws SQLException {
        var schema = delegate.load();
        var filtered = schema.tables().stream()
            .filter(table -> matches(table.schema(), schemaIncludes, schemaExcludes))
            .filter(table -> matches(table.name(), tableIncludes, tableExcludes))
            .toList();
        return CatalogSchema.of(filtered);
    }

    private static boolean matches(String value, List<Pattern> includes, List<Pattern> excludes) {
        var candidate = value == null ? "" : value;
        if (!includes.isEmpty() && includes.stream().noneMatch(pattern -> pattern.matcher(candidate).matches())) {
            return false;
        }
        return excludes.stream().noneMatch(pattern -> pattern.matcher(candidate).matches());
    }
}

