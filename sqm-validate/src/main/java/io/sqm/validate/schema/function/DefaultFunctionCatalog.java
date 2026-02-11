package io.sqm.validate.schema.function;

import io.sqm.validate.schema.model.DbType;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Default built-in function catalog used by schema validator.
 */
public final class DefaultFunctionCatalog implements FunctionCatalog {
    private final Map<String, FunctionSignature> signatures;

    private DefaultFunctionCatalog(Map<String, FunctionSignature> signatures) {
        this.signatures = Map.copyOf(signatures);
    }

    /**
     * Creates standard built-in catalog.
     *
     * @return default catalog.
     */
    public static DefaultFunctionCatalog standard() {
        return new DefaultFunctionCatalog(Map.ofEntries(
            Map.entry("count", FunctionSignature.ofAggregate(1, 1, DbType.LONG, FunctionArgKind.STAR_OR_EXPR)),
            Map.entry("lower", FunctionSignature.of(1, 1, DbType.STRING, FunctionArgKind.STRING_EXPR)),
            Map.entry("upper", FunctionSignature.of(1, 1, DbType.STRING, FunctionArgKind.STRING_EXPR)),
            Map.entry("length", FunctionSignature.of(1, 1, DbType.INTEGER, FunctionArgKind.STRING_EXPR)),
            Map.entry("char_length", FunctionSignature.of(1, 1, DbType.INTEGER, FunctionArgKind.STRING_EXPR)),
            Map.entry("sum", FunctionSignature.ofAggregate(1, 1, DbType.DECIMAL, FunctionArgKind.NUMERIC_EXPR)),
            Map.entry("avg", FunctionSignature.ofAggregate(1, 1, DbType.DECIMAL, FunctionArgKind.NUMERIC_EXPR)),
            Map.entry("abs", FunctionSignature.of(1, 1, DbType.DECIMAL, FunctionArgKind.NUMERIC_EXPR)),
            Map.entry("min", FunctionSignature.ofAggregate(1, 1, FunctionArgKind.ANY_EXPR)),
            Map.entry("max", FunctionSignature.ofAggregate(1, 1, FunctionArgKind.ANY_EXPR)),
            Map.entry("array_agg", FunctionSignature.ofAggregate(1, 1, FunctionArgKind.ANY_EXPR)),
            Map.entry("string_agg", FunctionSignature.ofAggregate(2, 2, FunctionArgKind.ANY_EXPR, FunctionArgKind.ANY_EXPR)),
            Map.entry("json_agg", FunctionSignature.ofAggregate(1, 1, DbType.JSON, FunctionArgKind.ANY_EXPR)),
            Map.entry("jsonb_agg", FunctionSignature.ofAggregate(1, 1, DbType.JSONB, FunctionArgKind.ANY_EXPR)),
            Map.entry("bool_and", FunctionSignature.ofAggregate(1, 1, DbType.BOOLEAN, FunctionArgKind.ANY_EXPR)),
            Map.entry("bool_or", FunctionSignature.ofAggregate(1, 1, DbType.BOOLEAN, FunctionArgKind.ANY_EXPR)),
            Map.entry("every", FunctionSignature.ofAggregate(1, 1, DbType.BOOLEAN, FunctionArgKind.ANY_EXPR)),
            Map.entry("stddev", FunctionSignature.ofAggregate(1, 1, DbType.DECIMAL, FunctionArgKind.NUMERIC_EXPR)),
            Map.entry("stddev_pop", FunctionSignature.ofAggregate(1, 1, DbType.DECIMAL, FunctionArgKind.NUMERIC_EXPR)),
            Map.entry("stddev_samp", FunctionSignature.ofAggregate(1, 1, DbType.DECIMAL, FunctionArgKind.NUMERIC_EXPR)),
            Map.entry("variance", FunctionSignature.ofAggregate(1, 1, DbType.DECIMAL, FunctionArgKind.NUMERIC_EXPR)),
            Map.entry("var_pop", FunctionSignature.ofAggregate(1, 1, DbType.DECIMAL, FunctionArgKind.NUMERIC_EXPR)),
            Map.entry("var_samp", FunctionSignature.ofAggregate(1, 1, DbType.DECIMAL, FunctionArgKind.NUMERIC_EXPR)),
            Map.entry("coalesce", FunctionSignature.of(2, Integer.MAX_VALUE, FunctionArgKind.ANY_EXPR))
        ));
    }

    /**
     * Creates catalog from provided signatures map.
     *
     * @param signatures signatures keyed by function name.
     * @return catalog instance.
     */
    public static DefaultFunctionCatalog of(Map<String, FunctionSignature> signatures) {
        Objects.requireNonNull(signatures, "signatures");
        return new DefaultFunctionCatalog(signatures);
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
        return Optional.ofNullable(signatures.get(functionName.toLowerCase(Locale.ROOT)));
    }
}
