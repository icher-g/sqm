package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.List;
import java.util.Objects;

/**
 * ANSI table value constructor: (VALUES (...), (...)) [AS alias(col1, col2, ...)]
 */
public non-sealed interface ValuesTable extends AliasedTableRef {

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
    static ValuesTable of(RowValues rows) {
        return new Impl(rows, null, null);
    }

    /**
     * Rows of expressions; all rows must have the same arity.
     */
    RowValues values();

    /**
     * Adds column names to the alias.
     * Note: column names without {@link ValuesTable#alias()} are ignored.
     *
     * @param columnAliases a list of column names to add.
     * @return this.
     */
    default ValuesTable columnAliases(List<String> columnAliases) {
        return new Impl(values(), Objects.requireNonNull(columnAliases), alias());
    }

    /**
     * Adds column names to the alias.
     * Note: column names without {@link ValuesTable#alias()} are ignored.
     *
     * @param columnAliases a list of column names to add.
     * @return this.
     */
    default ValuesTable columnAliases(String... columnAliases) {
        return new Impl(values(), List.of(columnAliases), alias());
    }

    /**
     * Adds alias to the values table.
     *
     * @param alias an alias to add.
     * @return A newly created values table with the provide alias. All other fields are preserved.
     */
    default ValuesTable as(String alias) {
        return new Impl(values(), columnAliases(), Objects.requireNonNull(alias));
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

    /**
     * ANSI table value constructor: (VALUES (...), (...)) [AS alias(col1, col2, ...)]
     *
     * @param values        Rows of expressions; all rows must have the same arity.
     * @param columnAliases Optional derived column list; may be null or empty.
     * @param alias         table alias or null if none
     */
    record Impl(RowValues values, List<String> columnAliases, String alias) implements ValuesTable {

        public Impl {
            columnAliases = columnAliases == null ? null : List.copyOf(columnAliases);
        }
    }
}
