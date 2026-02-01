package io.sqm.core.dialect;

/**
 * Enumerates SQL features that may or may not be supported by a dialect.
 * <p>
 * Capabilities are used by parsers and renderers to accept or reject
 * dialect-specific syntax without encoding dialect logic into the core model.
 */
public enum SqlFeature {
    DATE_TYPED_LITERAL("DATE typed literal"),
    TIME_TYPED_LITERAL("TIME typed literal"),
    TIMESTAMP_TYPED_LITERAL("TIMESTAMP typed literal"),
    INTERVAL_LITERAL("INTERVAL typed literal"),
    BIT_STRING_LITERAL("BIT string literal"),
    HEX_STRING_LITERAL("HEX string literal"),
    ESCAPE_STRING_LITERAL("PostgreSQL E'...' string literal"),
    DOLLAR_STRING_LITERAL("PostgreSQL $tag$...$tag$ string literal"),
    DISTINCT_ON("PostgreSQL DISTINCT ON"),
    ORDER_BY_USING("PostgreSQL ORDER BY ... USING <operator>"),
    CTE_MATERIALIZATION("PostgreSQL CTE MATERIALIZED/NOT MATERIALIZED"),
    LOCKING_CLAUSE("FOR UPDATE locking clause"),
    LOCKING_SHARE("FOR SHARE locking mode"),
    LOCKING_KEY_SHARE("FOR KEY SHARE locking mode"),
    LOCKING_NO_KEY_UPDATE("FOR NO KEY UPDATE locking mode"),
    LOCKING_OF("FOR UPDATE OF <tables>"),
    LOCKING_NOWAIT("NOWAIT locking modifier"),
    LOCKING_SKIP_LOCKED("SKIP LOCKED locking modifier"),
    TABLE_INHERITANCE_ONLY("PostgreSQL ONLY table inheritance modifier"),
    TABLE_INHERITANCE_DESCENDANTS("PostgreSQL table * inheritance modifier"),
    LATERAL("LATERAL table reference"),
    FUNCTION_TABLE("Set-returning function in FROM"),
    FUNCTION_TABLE_ORDINALITY("WITH ORDINALITY for function tables"),
    GROUPING_SETS("GROUPING SETS"),
    ROLLUP("ROLLUP"),
    CUBE("CUBE"),
    ILIKE_PREDICATE("PostgreSQL ILIKE predicate"),
    SIMILAR_TO_PREDICATE("PostgreSQL SIMILAR TO predicate"),
    IS_DISTINCT_FROM_PREDICATE("PostgreSQL IS DISTINCT FROM predicate"),
    REGEX_PREDICATE("PostgreSQL regex predicate"),
    POSTGRES_TYPECAST("PostgreSQL :: typecast"),
    ARRAY_LITERAL("PostgreSQL array literal"),
    ARRAY_SUBSCRIPT("PostgreSQL array subscript"),
    ARRAY_SLICE("PostgreSQL array slice"),
    CUSTOM_OPERATOR("Dialect-specific binary operators");

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
