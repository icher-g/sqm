package io.sqm.core.transform;

import java.util.List;

/**
 * Resolves a qualifier for an unqualified column name using visible table sources in scope.
 */
@FunctionalInterface
public interface ColumnQualificationResolver {
    /**
     * Default resolver that leaves all columns unresolved (no qualification).
     */
    ColumnQualificationResolver NO_OP = (columnName, visibleTables) -> ColumnQualification.unresolved();

    /**
     * Resolves qualification outcome for an unqualified column.
     *
     * @param columnName unqualified column name
     * @param visibleTables visible table bindings in nearest-to-outer scope order
     * @return qualification outcome
     */
    ColumnQualification resolve(String columnName, List<VisibleTableBinding> visibleTables);
}
