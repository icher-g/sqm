package io.sqm.catalog.access;

/**
 * Principal-aware access policy for catalog-backed SQL validation.
 */
public interface CatalogAccessPolicy {
    /**
     * Returns true when the table is denied for the given principal.
     *
     * @param principal principal identifier, may be {@code null}.
     * @param schemaName table schema, may be {@code null}.
     * @param tableName table name.
     * @return true when denied.
     */
    boolean isTableDenied(String principal, String schemaName, String tableName);

    /**
     * Returns true when the source column is denied for the given principal.
     *
     * @param principal principal identifier, may be {@code null}.
     * @param sourceName source alias or table name, may be {@code null}.
     * @param columnName column name.
     * @return true when denied.
     */
    boolean isColumnDenied(String principal, String sourceName, String columnName);

    /**
     * Returns true when the function is allowed for the given principal.
     *
     * @param principal principal identifier, may be {@code null}.
     * @param functionName function name.
     * @return true when allowed.
     */
    boolean isFunctionAllowed(String principal, String functionName);

    /**
     * Returns true when the table is denied without principal context.
     *
     * @param schemaName table schema, may be {@code null}.
     * @param tableName table name.
     * @return true when denied.
     */
    default boolean isTableDenied(String schemaName, String tableName) {
        return isTableDenied(null, schemaName, tableName);
    }

    /**
     * Returns true when the source column is denied without principal context.
     *
     * @param sourceName source alias or table name, may be {@code null}.
     * @param columnName column name.
     * @return true when denied.
     */
    default boolean isColumnDenied(String sourceName, String columnName) {
        return isColumnDenied(null, sourceName, columnName);
    }

    /**
     * Returns true when the function is allowed without principal context.
     *
     * @param functionName function name.
     * @return true when allowed.
     */
    default boolean isFunctionAllowed(String functionName) {
        return isFunctionAllowed(null, functionName);
    }
}

