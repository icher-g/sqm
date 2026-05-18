package io.sqm.core.walk;

import io.sqm.core.JsonTableBehavior;
import io.sqm.core.JsonTableExistsColumn;
import io.sqm.core.JsonTableNestedPathColumn;
import io.sqm.core.JsonTableOrdinalityColumn;
import io.sqm.core.JsonTableScalarColumn;

/**
 * Visitor for {@code JSON_TABLE} column and behavior nodes.
 *
 * @param <R> the result type produced by the visitor
 */
public interface JsonTableVisitor<R> {
    /**
     * Visits a scalar JSON table column.
     *
     * @param column scalar JSON table column
     * @return visitor result
     */
    R visitJsonTableScalarColumn(JsonTableScalarColumn column);

    /**
     * Visits an ordinality JSON table column.
     *
     * @param column ordinality JSON table column
     * @return visitor result
     */
    R visitJsonTableOrdinalityColumn(JsonTableOrdinalityColumn column);

    /**
     * Visits an existence-test JSON table column.
     *
     * @param column existence-test JSON table column
     * @return visitor result
     */
    R visitJsonTableExistsColumn(JsonTableExistsColumn column);

    /**
     * Visits a nested-path JSON table column group.
     *
     * @param column nested-path JSON table column group
     * @return visitor result
     */
    R visitJsonTableNestedPathColumn(JsonTableNestedPathColumn column);

    /**
     * Visits an empty/error JSON table behavior.
     *
     * @param behavior JSON table behavior
     * @return visitor result
     */
    R visitJsonTableBehavior(JsonTableBehavior behavior);
}
