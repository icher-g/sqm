package io.sqm.catalog.model;

import java.util.Objects;

/**
 * Catalog column metadata.
 *
 * @param name column name.
 * @param type semantic column type.
 */
public record CatalogColumn(String name, CatalogType type) {
    /**
     * Validates constructor arguments.
     *
     * @param name column name.
     * @param type semantic type.
     */
    public CatalogColumn {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
    }

    /**
     * Creates catalog column.
     *
     * @param name column name.
     * @param type semantic type.
     * @return catalog column.
     */
    public static CatalogColumn of(String name, CatalogType type) {
        return new CatalogColumn(name, type);
    }
}
