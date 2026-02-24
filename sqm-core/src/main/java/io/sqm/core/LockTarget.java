package io.sqm.core;

import java.util.Objects;

/**
 * Identifier used in PostgreSQL FOR UPDATE / FOR SHARE OF clause.
 * Represents a table name or table alias visible in FROM.
 *
 * @param identifier Identifier used in FOR UPDATE OF clause. This is either a table name or an alias.
 */
public record LockTarget(Identifier identifier) {

    /**
     * Creates a lock target.
     *
     * @param identifier table name or alias identifier
     */
    public LockTarget {
        Objects.requireNonNull(identifier, "identifier");
    }

    /**
     * Creates a lock target from a quote-aware identifier.
     *
     * @param identifier table name or alias identifier
     * @return lock target
     */
    public static LockTarget of(Identifier identifier) {
        return new LockTarget(identifier);
    }
}

