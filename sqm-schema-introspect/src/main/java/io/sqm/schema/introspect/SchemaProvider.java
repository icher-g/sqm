package io.sqm.schema.introspect;

import io.sqm.validate.schema.model.DbSchema;

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
    DbSchema load() throws SQLException;
}

