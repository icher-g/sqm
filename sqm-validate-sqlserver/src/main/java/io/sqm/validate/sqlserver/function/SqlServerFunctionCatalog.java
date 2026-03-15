package io.sqm.validate.sqlserver.function;

import io.sqm.catalog.model.CatalogType;
import io.sqm.validate.schema.function.DefaultFunctionCatalog;
import io.sqm.validate.schema.function.FunctionArgKind;
import io.sqm.validate.schema.function.FunctionCatalog;
import io.sqm.validate.schema.function.FunctionSignature;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * SQL Server-oriented function catalog for schema validation.
 *
 * <p>This catalog augments the shared default function catalog with the
 * baseline SQL Server function slice delivered in R5.</p>
 */
public final class SqlServerFunctionCatalog implements FunctionCatalog {
    private final FunctionCatalog fallback;
    private final Map<String, FunctionSignature> signatures;

    private SqlServerFunctionCatalog(FunctionCatalog fallback, Map<String, FunctionSignature> signatures) {
        this.fallback = Objects.requireNonNull(fallback, "fallback");
        this.signatures = Map.copyOf(signatures);
    }

    /**
     * Creates the standard SQL Server function catalog.
     *
     * @return SQL Server function catalog.
     */
    public static SqlServerFunctionCatalog standard() {
        return withFallback(DefaultFunctionCatalog.standard());
    }

    /**
     * Creates the SQL Server function catalog with a custom fallback catalog.
     *
     * @param fallback fallback catalog for functions not declared by SQL Server catalog.
     * @return SQL Server function catalog.
     */
    public static SqlServerFunctionCatalog withFallback(FunctionCatalog fallback) {
        Objects.requireNonNull(fallback, "fallback");
        return new SqlServerFunctionCatalog(fallback, Map.ofEntries(
            Map.entry("len", FunctionSignature.of(1, 1, CatalogType.INTEGER, FunctionArgKind.STRING_EXPR)),
            Map.entry("datalength", FunctionSignature.of(1, 1, CatalogType.INTEGER, FunctionArgKind.ANY_EXPR)),
            Map.entry("getdate", FunctionSignature.of(0, 0, CatalogType.TIMESTAMP)),
            Map.entry("dateadd", FunctionSignature.of(3, 3,
                FunctionArgKind.STRING_EXPR,
                FunctionArgKind.NUMERIC_EXPR,
                FunctionArgKind.ANY_EXPR)),
            Map.entry("datediff", FunctionSignature.of(3, 3, CatalogType.INTEGER,
                FunctionArgKind.STRING_EXPR,
                FunctionArgKind.ANY_EXPR,
                FunctionArgKind.ANY_EXPR)),
            Map.entry("isnull", FunctionSignature.of(2, 2, FunctionArgKind.ANY_EXPR, FunctionArgKind.ANY_EXPR)),
            Map.entry("string_agg", FunctionSignature.ofAggregate(2, 2, CatalogType.STRING,
                FunctionArgKind.ANY_EXPR,
                FunctionArgKind.ANY_EXPR))
        ));
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
