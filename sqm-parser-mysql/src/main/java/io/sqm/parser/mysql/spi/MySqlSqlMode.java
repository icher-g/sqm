package io.sqm.parser.mysql.spi;

/**
 * Enumerates MySQL SQL modes that affect parser behavior.
 */
public enum MySqlSqlMode {
    /**
     * Treats double-quoted tokens as identifiers, matching MySQL {@code ANSI_QUOTES} mode.
     */
    ANSI_QUOTES
}
