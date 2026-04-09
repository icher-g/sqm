package io.sqm.validate.schema;

import io.sqm.catalog.model.CatalogSchema;

/**
 * Shared catalog-schema factories for validator use cases.
 */
public final class ValidationCatalogSchemas {

    private ValidationCatalogSchemas() {
    }

    /**
     * Returns a permissive schema that accepts any table and column reference.
     *
     * @return permissive catalog schema.
     */
    public static CatalogSchema allowEverything() {
        return CatalogSchema.allowEverything();
    }
}
