package io.sqm.core.transform;

/**
 * Resolves schema qualification for unqualified table names.
 */
@FunctionalInterface
public interface TableSchemaResolver {
    /**
     * Default resolver that keeps all tables unresolved (no qualification).
     */
    TableSchemaResolver NO_OP = tableName -> TableQualification.unresolved();

    /**
     * Resolves qualification outcome for table name.
     *
     * @param tableName unqualified table name.
     * @return qualification outcome.
     */
    TableQualification resolve(String tableName);
}
