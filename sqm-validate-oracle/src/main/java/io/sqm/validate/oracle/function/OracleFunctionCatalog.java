package io.sqm.validate.oracle.function;

import io.sqm.catalog.model.CatalogType;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.validate.schema.function.DefaultFunctionCatalog;
import io.sqm.validate.schema.function.FunctionArgKind;
import io.sqm.validate.schema.function.FunctionCatalog;
import io.sqm.validate.schema.function.FunctionSignature;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.HashMap;

/**
 * Oracle-oriented function catalog for schema validation.
 *
 * <p>This catalog augments the shared default catalog with a practical Oracle
 * 19c baseline of scalar, string, numeric, date/time, null-handling, and
 * aggregate function signatures.</p>
 */
public final class OracleFunctionCatalog implements FunctionCatalog {
    private static final SqlDialectVersion LATEST_SUPPORTED = SqlDialectVersion.of(19, 0);

    private final FunctionCatalog fallback;
    private final Map<String, FunctionSignature> signatures;

    private OracleFunctionCatalog(FunctionCatalog fallback, Map<String, FunctionSignature> signatures) {
        this.fallback = Objects.requireNonNull(fallback, "fallback");
        this.signatures = Map.copyOf(signatures);
    }

    /**
     * Creates the standard Oracle function catalog for the latest supported version.
     *
     * @return Oracle function catalog.
     */
    public static OracleFunctionCatalog standard() {
        return of(LATEST_SUPPORTED);
    }

    /**
     * Creates an Oracle function catalog for a specific Oracle version.
     *
     * @param version Oracle version.
     * @return Oracle function catalog.
     */
    public static OracleFunctionCatalog of(SqlDialectVersion version) {
        Objects.requireNonNull(version, "version");
        return new OracleFunctionCatalog(DefaultFunctionCatalog.standard(), signaturesFor(version));
    }

    /**
     * Creates an Oracle function catalog using a custom fallback catalog.
     *
     * @param fallback fallback catalog for functions not declared by the Oracle catalog.
     * @return Oracle function catalog.
     */
    public static OracleFunctionCatalog withFallback(FunctionCatalog fallback) {
        return withFallback(fallback, LATEST_SUPPORTED);
    }

    /**
     * Creates an Oracle function catalog for a specific version using a custom fallback catalog.
     *
     * @param fallback fallback catalog for functions not declared by the Oracle catalog.
     * @param version  Oracle version.
     * @return Oracle function catalog.
     */
    public static OracleFunctionCatalog withFallback(FunctionCatalog fallback, SqlDialectVersion version) {
        Objects.requireNonNull(fallback, "fallback");
        Objects.requireNonNull(version, "version");
        return new OracleFunctionCatalog(fallback, signaturesFor(version));
    }

    private static Map<String, FunctionSignature> signaturesFor(SqlDialectVersion version) {
        Objects.requireNonNull(version, "version");
        var signatures = new HashMap<String, FunctionSignature>();
        var oracle8 = SqlDialectVersion.of(8, 0);
        var oracle9 = SqlDialectVersion.of(9, 0);
        var oracle10 = SqlDialectVersion.of(10, 0);
        var oracle11 = SqlDialectVersion.of(11, 2);
        var oracle12 = SqlDialectVersion.of(12, 1);

        put(signatures, version, oracle8, "nvl",
            FunctionSignature.of(2, 2, FunctionArgKind.ANY_EXPR, FunctionArgKind.ANY_EXPR));
        put(signatures, version, oracle8, "nvl2",
            FunctionSignature.of(3, 3, FunctionArgKind.ANY_EXPR, FunctionArgKind.ANY_EXPR, FunctionArgKind.ANY_EXPR));
        put(signatures, version, oracle8, "decode",
            FunctionSignature.of(3, Integer.MAX_VALUE, FunctionArgKind.ANY_EXPR));
        put(signatures, version, oracle8, "nullif",
            FunctionSignature.of(2, 2, FunctionArgKind.ANY_EXPR, FunctionArgKind.ANY_EXPR));
        put(signatures, version, oracle8, "substr",
            FunctionSignature.of(2, 3, CatalogType.STRING, FunctionArgKind.STRING_EXPR, FunctionArgKind.NUMERIC_EXPR, FunctionArgKind.NUMERIC_EXPR));
        put(signatures, version, oracle8, "substring",
            FunctionSignature.of(2, 3, CatalogType.STRING, FunctionArgKind.STRING_EXPR, FunctionArgKind.NUMERIC_EXPR, FunctionArgKind.NUMERIC_EXPR));
        put(signatures, version, oracle8, "instr",
            FunctionSignature.of(2, 4, CatalogType.INTEGER, FunctionArgKind.STRING_EXPR, FunctionArgKind.STRING_EXPR, FunctionArgKind.NUMERIC_EXPR, FunctionArgKind.NUMERIC_EXPR));
        put(signatures, version, oracle8, "replace",
            FunctionSignature.of(2, 3, CatalogType.STRING, FunctionArgKind.STRING_EXPR, FunctionArgKind.STRING_EXPR, FunctionArgKind.STRING_EXPR));
        put(signatures, version, oracle10, "regexp_replace",
            FunctionSignature.of(3, 6, CatalogType.STRING, FunctionArgKind.STRING_EXPR, FunctionArgKind.STRING_EXPR, FunctionArgKind.STRING_EXPR, FunctionArgKind.NUMERIC_EXPR, FunctionArgKind.NUMERIC_EXPR, FunctionArgKind.STRING_EXPR));
        put(signatures, version, oracle10, "regexp_substr",
            FunctionSignature.of(2, 6, CatalogType.STRING, FunctionArgKind.STRING_EXPR, FunctionArgKind.STRING_EXPR, FunctionArgKind.NUMERIC_EXPR, FunctionArgKind.NUMERIC_EXPR, FunctionArgKind.STRING_EXPR, FunctionArgKind.NUMERIC_EXPR));
        put(signatures, version, oracle8, "lengthb",
            FunctionSignature.of(1, 1, CatalogType.INTEGER, FunctionArgKind.STRING_EXPR));
        put(signatures, version, oracle8, "chr",
            FunctionSignature.of(1, 1, CatalogType.STRING, FunctionArgKind.NUMERIC_EXPR));
        put(signatures, version, oracle8, "ascii",
            FunctionSignature.of(1, 1, CatalogType.INTEGER, FunctionArgKind.STRING_EXPR));
        put(signatures, version, oracle8, "concat",
            FunctionSignature.of(2, 2, CatalogType.STRING, FunctionArgKind.ANY_EXPR, FunctionArgKind.ANY_EXPR));
        put(signatures, version, oracle8, "to_char",
            FunctionSignature.of(1, 3, CatalogType.STRING, FunctionArgKind.ANY_EXPR, FunctionArgKind.STRING_EXPR, FunctionArgKind.STRING_EXPR));
        put(signatures, version, oracle8, "to_date",
            FunctionSignature.of(1, 3, CatalogType.DATE, FunctionArgKind.STRING_EXPR, FunctionArgKind.STRING_EXPR, FunctionArgKind.STRING_EXPR));
        put(signatures, version, oracle9, "to_timestamp",
            FunctionSignature.of(1, 3, CatalogType.TIMESTAMP, FunctionArgKind.STRING_EXPR, FunctionArgKind.STRING_EXPR, FunctionArgKind.STRING_EXPR));
        put(signatures, version, oracle8, "to_number",
            FunctionSignature.of(1, 3, CatalogType.DECIMAL, FunctionArgKind.ANY_EXPR, FunctionArgKind.STRING_EXPR, FunctionArgKind.STRING_EXPR));
        put(signatures, version, oracle8, "sysdate",
            FunctionSignature.of(0, 0, CatalogType.TIMESTAMP));
        put(signatures, version, oracle9, "systimestamp",
            FunctionSignature.of(0, 0, CatalogType.TIMESTAMP));
        put(signatures, version, oracle8, "current_date",
            FunctionSignature.of(0, 0, CatalogType.DATE));
        put(signatures, version, oracle9, "current_timestamp",
            FunctionSignature.of(0, 0, CatalogType.TIMESTAMP));
        put(signatures, version, oracle8, "add_months",
            FunctionSignature.of(2, 2, CatalogType.DATE, FunctionArgKind.ANY_EXPR, FunctionArgKind.NUMERIC_EXPR));
        put(signatures, version, oracle8, "months_between",
            FunctionSignature.of(2, 2, CatalogType.DECIMAL, FunctionArgKind.ANY_EXPR, FunctionArgKind.ANY_EXPR));
        put(signatures, version, oracle8, "last_day",
            FunctionSignature.of(1, 1, CatalogType.DATE, FunctionArgKind.ANY_EXPR));
        put(signatures, version, oracle8, "round",
            FunctionSignature.of(1, 2, CatalogType.DECIMAL, FunctionArgKind.NUMERIC_EXPR, FunctionArgKind.NUMERIC_EXPR));
        put(signatures, version, oracle8, "trunc",
            FunctionSignature.of(1, 2, FunctionArgKind.ANY_EXPR, FunctionArgKind.ANY_EXPR));
        put(signatures, version, oracle8, "ceil",
            FunctionSignature.of(1, 1, CatalogType.DECIMAL, FunctionArgKind.NUMERIC_EXPR));
        put(signatures, version, oracle8, "floor",
            FunctionSignature.of(1, 1, CatalogType.DECIMAL, FunctionArgKind.NUMERIC_EXPR));
        put(signatures, version, oracle8, "power",
            FunctionSignature.of(2, 2, CatalogType.DECIMAL, FunctionArgKind.NUMERIC_EXPR, FunctionArgKind.NUMERIC_EXPR));
        put(signatures, version, oracle8, "mod",
            FunctionSignature.of(2, 2, CatalogType.DECIMAL, FunctionArgKind.NUMERIC_EXPR, FunctionArgKind.NUMERIC_EXPR));
        put(signatures, version, oracle11, "listagg",
            FunctionSignature.ofAggregate(1, 2, CatalogType.STRING, FunctionArgKind.ANY_EXPR, FunctionArgKind.STRING_EXPR));
        put(signatures, version, oracle10, "median",
            FunctionSignature.ofAggregate(1, 1, CatalogType.DECIMAL, FunctionArgKind.NUMERIC_EXPR));
        put(signatures, version, oracle8, "ratio_to_report",
            FunctionSignature.of(1, 1, CatalogType.DECIMAL, FunctionArgKind.NUMERIC_EXPR));
        put(signatures, version, oracle12, "json_value",
            FunctionSignature.of(2, 2, CatalogType.STRING, FunctionArgKind.ANY_EXPR, FunctionArgKind.STRING_EXPR));
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
