package io.sqm.core.dialect;

/**
 * Enumerates SQL features that may or may not be supported by a dialect.
 * <p>
 * Capabilities are used by parsers and renderers to accept or reject
 * dialect-specific syntax without encoding dialect logic into the core model.
 */
public enum SqlFeature {
    /**
     * DATE typed literal such as {@code DATE '2024-01-01'}.
     */
    DATE_TYPED_LITERAL("DATE typed literal"),
    /**
     * TIME typed literal such as {@code TIME '12:30:45'}.
     */
    TIME_TYPED_LITERAL("TIME typed literal"),
    /**
     * TIMESTAMP typed literal such as {@code TIMESTAMP '2024-01-01 12:30:45'}.
     */
    TIMESTAMP_TYPED_LITERAL("TIMESTAMP typed literal"),
    /**
     * INTERVAL typed literal such as {@code INTERVAL '1 day'}.
     */
    INTERVAL_LITERAL("INTERVAL typed literal"),
    /**
     * BIT string literal such as {@code B'1010'}.
     */
    BIT_STRING_LITERAL("BIT string literal"),
    /**
     * HEX string literal such as {@code X'DEADBEEF'}.
     */
    HEX_STRING_LITERAL("HEX string literal"),
    /**
     * PostgreSQL escape string literal with E prefix such as {@code E'line1\nline2'}.
     */
    ESCAPE_STRING_LITERAL("PostgreSQL E'...' string literal"),
    /**
     * PostgreSQL dollar-quoted string literal such as {@code $$text$$} or {@code $tag$text$tag$}.
     */
    DOLLAR_STRING_LITERAL("PostgreSQL $tag$...$tag$ string literal"),
    /**
     * PostgreSQL DISTINCT ON clause for selecting distinct rows based on specific columns.
     */
    DISTINCT_ON("PostgreSQL DISTINCT ON"),
    /**
     * PostgreSQL ORDER BY ... USING operator syntax for custom sort ordering.
     */
    ORDER_BY_USING("PostgreSQL ORDER BY ... USING <operator>"),
    /**
     * PostgreSQL CTE MATERIALIZED/NOT MATERIALIZED optimization hints.
     */
    CTE_MATERIALIZATION("PostgreSQL CTE MATERIALIZED/NOT MATERIALIZED"),
    /**
     * FOR UPDATE locking clause for row-level locking.
     */
    LOCKING_CLAUSE("FOR UPDATE locking clause"),
    /**
     * FOR SHARE locking mode for shared row-level locks.
     */
    LOCKING_SHARE("FOR SHARE locking mode"),
    /**
     * FOR KEY SHARE locking mode (PostgreSQL-specific).
     */
    LOCKING_KEY_SHARE("FOR KEY SHARE locking mode"),
    /**
     * FOR NO KEY UPDATE locking mode (PostgreSQL-specific).
     */
    LOCKING_NO_KEY_UPDATE("FOR NO KEY UPDATE locking mode"),
    /**
     * FOR UPDATE OF tables syntax to specify which tables to lock.
     */
    LOCKING_OF("FOR UPDATE OF <tables>"),
    /**
     * NOWAIT locking modifier to fail immediately if lock cannot be acquired.
     */
    LOCKING_NOWAIT("NOWAIT locking modifier"),
    /**
     * SKIP LOCKED locking modifier to skip locked rows.
     */
    LOCKING_SKIP_LOCKED("SKIP LOCKED locking modifier"),
    /**
     * PostgreSQL ONLY table inheritance modifier to exclude child tables.
     */
    TABLE_INHERITANCE_ONLY("PostgreSQL ONLY table inheritance modifier"),
    /**
     * PostgreSQL table* inheritance modifier to include all descendants.
     */
    TABLE_INHERITANCE_DESCENDANTS("PostgreSQL table * inheritance modifier"),
    /**
     * LATERAL keyword for correlated table references in FROM clause.
     */
    LATERAL("LATERAL table reference"),
    /**
     * Set-returning functions in FROM clause that produce table output.
     */
    FUNCTION_TABLE("Set-returning function in FROM"),
    /**
     * WITH ORDINALITY clause for function tables to add row numbers.
     */
    FUNCTION_TABLE_ORDINALITY("WITH ORDINALITY for function tables"),
    /**
     * GROUPING SETS clause for advanced grouping in GROUP BY.
     */
    GROUPING_SETS("GROUPING SETS"),
    /**
     * ROLLUP grouping extension for generating subtotals.
     */
    ROLLUP("ROLLUP"),
    /**
     * CUBE grouping extension for generating all possible combinations.
     */
    CUBE("CUBE"),
    /**
     * PostgreSQL case-insensitive ILIKE pattern matching operator.
     */
    ILIKE_PREDICATE("PostgreSQL ILIKE predicate"),
    /**
     * PostgreSQL SIMILAR TO predicate for SQL pattern matching.
     */
    SIMILAR_TO_PREDICATE("PostgreSQL SIMILAR TO predicate"),
    /**
     * IS DISTINCT FROM predicate for null-safe comparison.
     */
    IS_DISTINCT_FROM_PREDICATE("PostgreSQL IS DISTINCT FROM predicate"),
    /**
     * PostgreSQL regex predicate operators (~, ~*, !~, !~*).
     */
    REGEX_PREDICATE("PostgreSQL regex predicate"),
    /**
     * PostgreSQL :: typecast operator syntax.
     */
    POSTGRES_TYPECAST("PostgreSQL :: typecast"),
    /**
     * PostgreSQL array literal constructor syntax such as {@code ARRAY[1, 2, 3]}.
     */
    ARRAY_LITERAL("PostgreSQL array literal"),
    /**
     * PostgreSQL array subscript operator for accessing array elements such as {@code arr[1]}.
     */
    ARRAY_SUBSCRIPT("PostgreSQL array subscript"),
    /**
     * PostgreSQL array slice operator for extracting subarrays such as {@code arr[1:3]}.
     */
    ARRAY_SLICE("PostgreSQL array slice"),
    /**
     * Dialect-specific binary operators not covered by standard SQL.
     */
    CUSTOM_OPERATOR("Dialect-specific binary operators"),
    /**
     * PostgreSQL AT TIME ZONE operator for timezone conversion.
     * Example: {@code timestamp_col AT TIME ZONE 'UTC'}
     */
    AT_TIME_ZONE("PostgreSQL AT TIME ZONE operator");

    private final String description;

    SqlFeature(String description) {
        this.description = description;
    }

    /**
     * Returns a human-friendly feature description.
     *
     * @return feature description
     */
    public String description() {
        return description;
    }
}
