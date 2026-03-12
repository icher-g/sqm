package io.sqm.transpile;

import io.sqm.catalog.model.CatalogSchema;
import io.sqm.core.dialect.SqlDialectId;

import java.util.Optional;

/**
 * Runtime context shared with transpilation rules.
 *
 * @param sourceDialect source dialect identifier
 * @param targetDialect target dialect identifier
 * @param options transpilation options
 * @param sourceSchema optional source schema
 * @param targetSchema optional target schema
 */
public record TranspileContext(
    SqlDialectId sourceDialect,
    SqlDialectId targetDialect,
    TranspileOptions options,
    Optional<CatalogSchema> sourceSchema,
    Optional<CatalogSchema> targetSchema
) {
}
