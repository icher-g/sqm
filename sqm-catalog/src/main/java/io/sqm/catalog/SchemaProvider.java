package io.sqm.catalog;

import io.sqm.catalog.model.CatalogSchema;

import java.sql.SQLException;

/**
 * Provides database schema metadata used by SQM validation and code generation.
 */
public interface SchemaProvider {
    /**
     * Loads schema metadata.
     *
     * @return loaded database schema model.
     * @throws SQLException if schema metadata cannot be read.
     */
    CatalogSchema load() throws SQLException;
}

