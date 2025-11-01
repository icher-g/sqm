package io.sqm.core.internal;

import io.sqm.core.RowListExpr;
import io.sqm.core.ValuesTable;

import java.util.List;

/**
 * ANSI table value constructor: (VALUES (...), (...)) [AS alias(col1, col2, ...)]
 *
 * @param rows        Rows of expressions; all rows must have the same arity.
 * @param columnNames Optional derived column list; may be null or empty.
 * @param alias       table alias or null if none
 */
public record ValuesTableImpl(RowListExpr rows, List<String> columnNames, String alias) implements ValuesTable {

    public ValuesTableImpl {
        columnNames = List.copyOf(columnNames);
    }
}
