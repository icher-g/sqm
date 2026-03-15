package io.sqm.core;

/**
 * SQL Server pseudo-row sources exposed by DML {@code OUTPUT} clauses.
 */
public enum OutputRowSource {
    /**
     * References the new row image produced by {@code INSERT} or {@code UPDATE}.
     */
    INSERTED,

    /**
     * References the old row image removed by {@code DELETE} or replaced by {@code UPDATE}.
     */
    DELETED
}
