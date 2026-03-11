package io.sqm.core.mysql.dialect;

import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.VersionedDialectCapabilities;

import java.util.Objects;

/**
 * Shared MySQL feature matrix used by parser and renderer modules.
 */
public final class MySqlCapabilities {
    private static final SqlDialectVersion LATEST_SUPPORTED = SqlDialectVersion.of(8, 0);

    private MySqlCapabilities() {
    }

    /**
     * Returns MySQL capabilities for the provided version.
     *
     * @param version MySQL version used to evaluate feature availability.
     * @return dialect capabilities for the provided version.
     */
    public static DialectCapabilities of(SqlDialectVersion version) {
        Objects.requireNonNull(version, "version");
        var mysql80 = SqlDialectVersion.of(8, 0);
        return VersionedDialectCapabilities.builder(version)
            .supports(mysql80,
                SqlFeature.DATE_TYPED_LITERAL,
                SqlFeature.TIME_TYPED_LITERAL,
                SqlFeature.TIMESTAMP_TYPED_LITERAL,
                SqlFeature.INTERVAL_LITERAL,
                SqlFeature.BIT_STRING_LITERAL,
                SqlFeature.HEX_STRING_LITERAL,
                SqlFeature.LOCKING_CLAUSE,
                SqlFeature.LOCKING_SHARE,
                SqlFeature.LOCKING_NOWAIT,
                SqlFeature.LOCKING_SKIP_LOCKED,
                SqlFeature.GROUPING_SETS,
                SqlFeature.ROLLUP,
                SqlFeature.CUBE,
                SqlFeature.NULL_SAFE_EQUALITY_PREDICATE,
                SqlFeature.REGEX_PREDICATE,
                SqlFeature.TABLE_INDEX_HINT,
                SqlFeature.CALC_FOUND_ROWS_MODIFIER,
                SqlFeature.OPTIMIZER_HINT_COMMENT,
                SqlFeature.INSERT_IGNORE,
                SqlFeature.INSERT_ON_DUPLICATE_KEY_UPDATE,
                SqlFeature.REPLACE_INTO,
                SqlFeature.UPDATE_JOIN,
                SqlFeature.STRAIGHT_JOIN,
                SqlFeature.DELETE_USING_JOIN
            )
            .build();
    }

    /**
     * Returns capabilities for the latest MySQL version supported by SQM.
     *
     * @return capabilities for the latest supported MySQL version.
     */
    public static DialectCapabilities latest() {
        return of(LATEST_SUPPORTED);
    }
}
