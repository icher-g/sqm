package io.sqm.core;

import java.util.Objects;

/**
 * Identifier used in PostgreSQL FOR UPDATE / FOR SHARE OF clause.
 * Represents a table name or table alias visible in FROM.
 *
 * @param identifier Identifier used in FOR UPDATE OF clause. This is either a table name or an alias.
 */
public record LockTarget(String identifier) {

    public LockTarget {
        Objects.requireNonNull(identifier, "identifier");
    }

    public static LockTarget of(String identifier) {
        return new LockTarget(identifier);
    }
}

