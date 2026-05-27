package io.sqm.core;

/**
 * Column definition inside a {@code JSON_TABLE COLUMNS (...)} clause.
 */
public sealed interface JsonTableColumn extends Node permits JsonTableScalarColumn, JsonTableOrdinalityColumn, JsonTableExistsColumn, JsonTableNestedPathColumn {
    /**
     * Returns the visible column name for non-nested column variants.
     *
     * @return column name, or {@code null} for nested path groups
     */
    Identifier name();
}
