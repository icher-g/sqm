package io.sqm.validate.postgresql.function;

import io.sqm.catalog.model.CatalogType;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.validate.schema.function.DefaultFunctionCatalog;
import io.sqm.validate.schema.function.FunctionArgKind;
import io.sqm.validate.schema.function.FunctionCatalog;
import io.sqm.validate.schema.function.FunctionSignature;

import java.util.*;

/**
 * PostgreSQL-oriented function catalog for schema validation.
 *
 * <p>This catalog augments and can override the default catalog with
 * PostgreSQL-specific signatures and return-type metadata.</p>
 */
public final class PostgresFunctionCatalog implements FunctionCatalog {
    private static final SqlDialectVersion LATEST_SUPPORTED = SqlDialectVersion.of(18, 0);

    private final FunctionCatalog fallback;
    private final Map<String, FunctionSignature> signatures;

    private PostgresFunctionCatalog(
        FunctionCatalog fallback,
        Map<String, FunctionSignature> signatures
    ) {
        this.fallback = Objects.requireNonNull(fallback, "fallback");
        this.signatures = Map.copyOf(signatures);
    }

    /**
     * Creates the standard PostgreSQL function catalog.
     *
     * @return PostgreSQL function catalog.
     */
    public static PostgresFunctionCatalog standard() {
        return of(LATEST_SUPPORTED);
    }

    /**
     * Creates PostgreSQL function catalog for a specific PostgreSQL version.
     *
     * @param version PostgreSQL version.
     * @return PostgreSQL function catalog.
     */
    public static PostgresFunctionCatalog of(SqlDialectVersion version) {
        Objects.requireNonNull(version, "version");
        return new PostgresFunctionCatalog(
            DefaultFunctionCatalog.standard(),
            signaturesFor(version)
        );
    }

    /**
     * Creates PostgreSQL function catalog using a custom fallback catalog.
     *
     * @param fallback fallback catalog for functions not declared by PostgreSQL catalog.
     * @return PostgreSQL function catalog.
     */
    public static PostgresFunctionCatalog withFallback(FunctionCatalog fallback) {
        Objects.requireNonNull(fallback, "fallback");
        return withFallback(fallback, LATEST_SUPPORTED);
    }

    /**
     * Creates PostgreSQL function catalog for a specific PostgreSQL version
     * using a custom fallback catalog.
     *
     * @param fallback fallback catalog for functions not declared by PostgreSQL catalog.
     * @param version  PostgreSQL version.
     * @return PostgreSQL function catalog.
     */
    public static PostgresFunctionCatalog withFallback(FunctionCatalog fallback, SqlDialectVersion version) {
        Objects.requireNonNull(fallback, "fallback");
        Objects.requireNonNull(version, "version");
        return new PostgresFunctionCatalog(fallback, signaturesFor(version));
    }

    private static Map<String, FunctionSignature> signaturesFor(SqlDialectVersion version) {
        var signatures = new HashMap<String, FunctionSignature>();
        put(signatures, version, SqlDialectVersion.of(9, 0), "unnest",
            FunctionSignature.of(1, 1, FunctionArgKind.ANY_EXPR));
        put(signatures, version, SqlDialectVersion.of(9, 0), "generate_series",
            FunctionSignature.of(2, 3, FunctionArgKind.NUMERIC_EXPR));
        put(signatures, version, SqlDialectVersion.of(9, 0), "pg_typeof",
            FunctionSignature.of(1, 1, CatalogType.STRING, FunctionArgKind.ANY_EXPR));
        put(signatures, version, SqlDialectVersion.of(9, 0), "array_length",
            FunctionSignature.of(2, 2, CatalogType.INTEGER, FunctionArgKind.ANY_EXPR, FunctionArgKind.NUMERIC_EXPR));
        put(signatures, version, SqlDialectVersion.of(9, 0), "array_dims",
            FunctionSignature.of(1, 1, CatalogType.STRING, FunctionArgKind.ANY_EXPR));
        put(signatures, version, SqlDialectVersion.of(9, 0), "strpos",
            FunctionSignature.of(2, 2, CatalogType.INTEGER, FunctionArgKind.STRING_EXPR, FunctionArgKind.STRING_EXPR));
        put(signatures, version, SqlDialectVersion.of(9, 0), "split_part",
            FunctionSignature.of(3, 3, CatalogType.STRING, FunctionArgKind.STRING_EXPR, FunctionArgKind.STRING_EXPR, FunctionArgKind.NUMERIC_EXPR));
        put(signatures, version, SqlDialectVersion.of(9, 0), "to_json",
            FunctionSignature.of(1, 1, CatalogType.JSON, FunctionArgKind.ANY_EXPR));
        put(signatures, version, SqlDialectVersion.of(9, 1), "concat",
            FunctionSignature.of(1, Integer.MAX_VALUE, CatalogType.STRING, FunctionArgKind.ANY_EXPR));
        put(signatures, version, SqlDialectVersion.of(9, 1), "format",
            FunctionSignature.of(1, Integer.MAX_VALUE, CatalogType.STRING, FunctionArgKind.STRING_EXPR, FunctionArgKind.ANY_EXPR));
        put(signatures, version, SqlDialectVersion.of(9, 2), "json_typeof",
            FunctionSignature.of(1, 1, CatalogType.STRING, FunctionArgKind.ANY_EXPR));
        put(signatures, version, SqlDialectVersion.of(9, 2), "array_to_json",
            FunctionSignature.of(1, 2, CatalogType.JSON, FunctionArgKind.ANY_EXPR, FunctionArgKind.ANY_EXPR));
        put(signatures, version, SqlDialectVersion.of(9, 2), "row_to_json",
            FunctionSignature.of(1, 2, CatalogType.JSON, FunctionArgKind.ANY_EXPR, FunctionArgKind.ANY_EXPR));
        put(signatures, version, SqlDialectVersion.of(9, 0), "string_agg",
            FunctionSignature.ofAggregate(2, 2, CatalogType.STRING, FunctionArgKind.ANY_EXPR, FunctionArgKind.ANY_EXPR));
        put(signatures, version, SqlDialectVersion.of(9, 4), "json_build_object",
            FunctionSignature.of(0, Integer.MAX_VALUE, CatalogType.JSON, FunctionArgKind.ANY_EXPR));
        put(signatures, version, SqlDialectVersion.of(9, 4), "json_object_agg",
            FunctionSignature.ofAggregate(2, 2, CatalogType.JSON, FunctionArgKind.ANY_EXPR, FunctionArgKind.ANY_EXPR));
        put(signatures, version, SqlDialectVersion.of(9, 4), "to_jsonb",
            FunctionSignature.of(1, 1, CatalogType.JSONB, FunctionArgKind.ANY_EXPR));
        put(signatures, version, SqlDialectVersion.of(9, 4), "jsonb_typeof",
            FunctionSignature.of(1, 1, CatalogType.STRING, FunctionArgKind.ANY_EXPR));
        put(signatures, version, SqlDialectVersion.of(9, 4), "jsonb_build_object",
            FunctionSignature.of(0, Integer.MAX_VALUE, CatalogType.JSONB, FunctionArgKind.ANY_EXPR));
        put(signatures, version, SqlDialectVersion.of(9, 4), "jsonb_build_array",
            FunctionSignature.of(0, Integer.MAX_VALUE, CatalogType.JSONB, FunctionArgKind.ANY_EXPR));
        put(signatures, version, SqlDialectVersion.of(9, 4), "jsonb_object_agg",
            FunctionSignature.ofAggregate(2, 2, CatalogType.JSONB, FunctionArgKind.ANY_EXPR, FunctionArgKind.ANY_EXPR));
        return signatures;
    }

    private static void put(
        Map<String, FunctionSignature> signatures,
        SqlDialectVersion version,
        SqlDialectVersion minVersion,
        String functionName,
        FunctionSignature signature
    ) {
        if (version.isAtLeast(minVersion)) {
            signatures.put(functionName, signature);
        }
    }

    /**
     * Resolves function signature by case-insensitive name.
     *
     * @param functionName function name.
     * @return resolved signature when known.
     */
    @Override
    public Optional<FunctionSignature> resolve(String functionName) {
        if (functionName == null) {
            return Optional.empty();
        }
        var key = functionName.toLowerCase(Locale.ROOT);
        var signature = signatures.get(key);
        if (signature != null) {
            return Optional.of(signature);
        }
        return fallback.resolve(functionName);
    }
}
