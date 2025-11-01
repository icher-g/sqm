package io.sqm.core;

import io.sqm.core.internal.ValuesTableImpl;
import io.sqm.core.walk.NodeVisitor;

import java.util.List;
import java.util.Objects;

/**
 * ANSI table value constructor: (VALUES (...), (...)) [AS alias(col1, col2, ...)]
 */
public non-sealed interface ValuesTable extends TableRef {

    /**
     * Creates a VALUES statement used as a table.
     * <p>For example:</p>
     * <pre>
     *     {@code
     *     (VALUES (1, 'A'), (2, 'B')) AS v(id, name)
     *     }
     * </pre>
     *
     * @param rows a set of values.
     * @return A newly created instance of the values table.
     */
    static ValuesTable of(RowListExpr rows) {
        return new ValuesTableImpl(rows, null, null);
    }

    /**
     * Rows of expressions; all rows must have the same arity.
     */
    RowListExpr rows();

    /**
     * Optional derived column list; may be null or empty.
     */
    List<String> columnNames();

    /**
     * Adds column names to the alias.
     * Note: column names without {@link ValuesTable#alias()} are ignored.
     *
     * @param columnNames a list of column names to add.
     * @return this.
     */
    default ValuesTable columnNames(List<String> columnNames) {
        return new ValuesTableImpl(rows(), Objects.requireNonNull(columnNames), alias());
    }

    /**
     * Adds alias to the values table.
     *
     * @param alias an alias to add.
     * @return A newly created values table with the provide alias. All other fields are preserved.
     */
    default ValuesTable as(String alias) {
        return new ValuesTableImpl(rows(), columnNames(), Objects.requireNonNull(alias));
    }

    /**
     * Accepts a {@link NodeVisitor} and dispatches control to the
     * visitor method corresponding to the concrete subtype
     *
     * @param v   the visitor instance to accept (must not be {@code null})
     * @param <R> the result type returned by the visitor
     * @return the result produced by the visitor
     */
    @Override
    default <R> R accept(NodeVisitor<R> v) {
        return v.visitValuesTable(this);
    }
}
