package io.sqm.catalog.model;

import java.util.List;
import java.util.Objects;

/**
 * Catalog foreign key metadata.
 *
 * @param name foreign key name, may be {@code null}.
 * @param sourceColumns local source columns.
 * @param targetSchema referenced schema, may be {@code null}.
 * @param targetTable referenced table name.
 * @param targetColumns referenced target columns.
 */
public record CatalogForeignKey(
    String name,
    List<String> sourceColumns,
    String targetSchema,
    String targetTable,
    List<String> targetColumns
) {
    /**
     * Validates constructor arguments.
     *
     * @param name foreign key name.
     * @param sourceColumns source columns.
     * @param targetSchema target schema.
     * @param targetTable target table.
     * @param targetColumns target columns.
     */
    public CatalogForeignKey {
        Objects.requireNonNull(sourceColumns, "sourceColumns");
        Objects.requireNonNull(targetTable, "targetTable");
        Objects.requireNonNull(targetColumns, "targetColumns");
        sourceColumns = List.copyOf(sourceColumns);
        targetColumns = List.copyOf(targetColumns);
    }

    /**
     * Creates foreign key metadata.
     *
     * @param name foreign key name.
     * @param sourceColumns source columns.
     * @param targetSchema target schema.
     * @param targetTable target table.
     * @param targetColumns target columns.
     * @return foreign key metadata.
     */
    public static CatalogForeignKey of(
        String name,
        List<String> sourceColumns,
        String targetSchema,
        String targetTable,
        List<String> targetColumns
    ) {
        return new CatalogForeignKey(name, sourceColumns, targetSchema, targetTable, targetColumns);
    }
}
