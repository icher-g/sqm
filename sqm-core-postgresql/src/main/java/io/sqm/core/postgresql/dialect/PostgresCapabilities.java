package io.sqm.core.postgresql.dialect;

import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.VersionedDialectCapabilities;

import java.util.Objects;

/**
 * Shared PostgreSQL feature matrix used by parser, renderer, and validator modules.
 */
public final class PostgresCapabilities {
    private static final SqlDialectVersion LATEST_SUPPORTED = SqlDialectVersion.of(18, 0);

    private PostgresCapabilities() {
    }

    /**
     * Returns PostgreSQL capabilities for the provided version.
     *
     * @param version PostgreSQL version used to evaluate feature availability.
     * @return dialect capabilities for the provided version.
     */
    public static DialectCapabilities of(SqlDialectVersion version) {
        Objects.requireNonNull(version, "version");
        var pg90 = SqlDialectVersion.of(9, 0);
        var pg93 = SqlDialectVersion.of(9, 3);
        var pg94 = SqlDialectVersion.of(9, 4);
        var pg95 = SqlDialectVersion.of(9, 5);
        var pg11 = SqlDialectVersion.of(11, 0);
        var pg12 = SqlDialectVersion.of(12, 0);
        return VersionedDialectCapabilities.builder(version)
            .supports(pg90,
                SqlFeature.DATE_TYPED_LITERAL,
                SqlFeature.TIME_TYPED_LITERAL,
                SqlFeature.TIMESTAMP_TYPED_LITERAL,
                SqlFeature.INTERVAL_LITERAL,
                SqlFeature.BIT_STRING_LITERAL,
                SqlFeature.HEX_STRING_LITERAL,
                SqlFeature.ESCAPE_STRING_LITERAL,
                SqlFeature.DOLLAR_STRING_LITERAL,
                SqlFeature.DISTINCT_ON,
                SqlFeature.ORDER_BY_USING,
                SqlFeature.LOCKING_CLAUSE,
                SqlFeature.LOCKING_SHARE,
                SqlFeature.LOCKING_OF,
                SqlFeature.LOCKING_NOWAIT,
                SqlFeature.TABLE_INHERITANCE_ONLY,
                SqlFeature.TABLE_INHERITANCE_DESCENDANTS,
                SqlFeature.FUNCTION_TABLE,
                SqlFeature.ILIKE_PREDICATE,
                SqlFeature.SIMILAR_TO_PREDICATE,
                SqlFeature.IS_DISTINCT_FROM_PREDICATE,
                SqlFeature.REGEX_PREDICATE,
                SqlFeature.POSTGRES_TYPECAST,
                SqlFeature.ARRAY_LITERAL,
                SqlFeature.ARRAY_SUBSCRIPT,
                SqlFeature.ARRAY_SLICE,
                SqlFeature.CUSTOM_OPERATOR,
                SqlFeature.AT_TIME_ZONE,
                SqlFeature.EXPR_COLLATE,
                SqlFeature.EXPONENTIATION_OPERATOR
            )
            .supports(pg93,
                SqlFeature.LATERAL,
                SqlFeature.LOCKING_KEY_SHARE,
                SqlFeature.LOCKING_NO_KEY_UPDATE
            )
            .supports(pg94, SqlFeature.FUNCTION_TABLE_ORDINALITY)
            .supports(pg95,
                SqlFeature.GROUPING_SETS,
                SqlFeature.ROLLUP,
                SqlFeature.CUBE,
                SqlFeature.LOCKING_SKIP_LOCKED
            )
            .supports(pg11,
                SqlFeature.WINDOW_FRAME_GROUPS,
                SqlFeature.WINDOW_FRAME_EXCLUDE
            )
            .supports(pg12, SqlFeature.CTE_MATERIALIZATION)
            .build();
    }

    /**
     * Returns capabilities for the latest PostgreSQL version supported by SQM.
     *
     * @return capabilities for the latest supported PostgreSQL version.
     */
    public static DialectCapabilities latest() {
        return of(LATEST_SUPPORTED);
    }
}
