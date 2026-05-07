package io.sqm.render.oracle.spi;

import io.sqm.render.ansi.spi.AnsiIdentifierQuoter;

import java.util.Set;

/**
 * Oracle identifier quoting rules.
 */
public class OracleIdentifierQuoter extends AnsiIdentifierQuoter {
    private static final Set<String> ORACLE_RESERVED = Set.of(
        "CONNECT", "START", "PRIOR", "SIBLINGS", "RETURNING", "INTO", "MERGE", "MODEL", "MINUS"
    );

    /**
     * Creates Oracle identifier quoter.
     */
    public OracleIdentifierQuoter() {
    }

    @Override
    public boolean needsQuoting(String identifier) {
        return super.needsQuoting(identifier) || ORACLE_RESERVED.contains(identifier.toUpperCase());
    }
}
