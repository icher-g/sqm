package io.sqm.core.oracle.dialect;

import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.VersionedDialectCapabilities;

import java.util.Objects;

/**
 * Shared Oracle feature matrix used by parser, renderer, and validator modules.
 */
public final class OracleCapabilities {
    private static final SqlDialectVersion ORACLE_12_1 = SqlDialectVersion.of(12, 1);
    private static final SqlDialectVersion ORACLE_19 = SqlDialectVersion.of(19, 0);
    private static final SqlDialectVersion LATEST_SUPPORTED = ORACLE_19;

    private OracleCapabilities() {
    }

    /**
     * Returns Oracle capabilities for the provided version.
     *
     * @param version Oracle version used to evaluate feature availability.
     * @return dialect capabilities for the provided version.
     */
    public static DialectCapabilities of(SqlDialectVersion version) {
        Objects.requireNonNull(version, "version");
        return VersionedDialectCapabilities.builder(version)
            .supports(SqlFeature.DATE_TYPED_LITERAL)
            .supports(SqlFeature.TIMESTAMP_TYPED_LITERAL)
            .supports(SqlFeature.INTERVAL_LITERAL)
            .supports(SqlFeature.DML_RESULT_CLAUSE)
            .supports(SqlFeature.DML_RESULT_VARIABLE_TARGET)
            .supports(SqlFeature.MERGE_STATEMENT)
            .supports(SqlFeature.GROUPING_SETS)
            .supports(SqlFeature.ROLLUP)
            .supports(SqlFeature.CUBE)
            .supports(SqlFeature.LOCKING_CLAUSE)
            .supports(SqlFeature.LOCKING_NOWAIT)
            .supports(SqlFeature.LOCKING_SKIP_LOCKED)
            .supports(SqlFeature.OPTIMIZER_HINT_COMMENT)
            .supports(SqlFeature.SEQUENCE_VALUE_EXPRESSION)
            .supports(SqlFeature.HIERARCHICAL_QUERY)
            .supports(SqlFeature.PIVOT_TABLE)
            .supports(SqlFeature.UNPIVOT_TABLE)
            .supports(SqlFeature.JSON_TABLE)
            .supports(ORACLE_12_1, SqlFeature.LATERAL)
            .build();
    }

    /**
     * Returns capabilities for the latest Oracle version supported by SQM.
     *
     * @return capabilities for the latest supported Oracle version.
     */
    public static DialectCapabilities latest() {
        return of(LATEST_SUPPORTED);
    }
}
