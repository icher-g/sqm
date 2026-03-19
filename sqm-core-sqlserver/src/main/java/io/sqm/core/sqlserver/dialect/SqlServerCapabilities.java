package io.sqm.core.sqlserver.dialect;

import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.VersionedDialectCapabilities;

import java.util.Objects;

/**
 * Shared SQL Server feature matrix used by parser and renderer modules.
 */
public final class SqlServerCapabilities {
    private static final SqlDialectVersion LATEST_SUPPORTED = SqlDialectVersion.of(2019, 0);

    private SqlServerCapabilities() {
    }

    /**
     * Returns SQL Server capabilities for the provided version.
     *
     * @param version SQL Server version used to evaluate feature availability.
     * @return dialect capabilities for the provided version.
     */
    public static DialectCapabilities of(SqlDialectVersion version) {
        Objects.requireNonNull(version, "version");
        return VersionedDialectCapabilities.builder(version)
            .supports(SqlFeature.DML_RESULT_CLAUSE)
            .supports(SqlFeature.EXPR_COLLATE)
            .build();
    }

    /**
     * Returns capabilities for the latest SQL Server version supported by SQM.
     *
     * @return capabilities for the latest supported SQL Server version.
     */
    public static DialectCapabilities latest() {
        return of(LATEST_SUPPORTED);
    }
}
