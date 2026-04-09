package io.sqm.playground.rest.service;

import io.sqm.core.dialect.SqlDialectId;
import io.sqm.playground.api.SqlDialectDto;

import java.util.Objects;

/**
 * Shared dialect conversion helpers for playground services.
 */
public final class PlaygroundDialectSupport {

    private PlaygroundDialectSupport() {
    }

    /**
     * Converts the transport dialect enum into the corresponding SQM dialect identifier.
     *
     * @param dialect transport dialect
     * @return SQM dialect identifier
     */
    public static SqlDialectId toDialectId(SqlDialectDto dialect) {
        Objects.requireNonNull(dialect, "dialect must not be null");
        return switch (dialect) {
            case ansi -> SqlDialectId.ANSI;
            case postgresql -> SqlDialectId.POSTGRESQL;
            case mysql -> SqlDialectId.MYSQL;
            case sqlserver -> SqlDialectId.SQLSERVER;
        };
    }
}
