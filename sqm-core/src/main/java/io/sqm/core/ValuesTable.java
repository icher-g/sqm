package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

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
     * Creates a VALUES statement used as a table with identifier metadata.
     *
     * @param rows          a set of values
     * @param alias         table alias identifier or null
     * @param columnAliases derived column aliases
     * @return a newly created values table
     */
    static ValuesTable of(RowValues rows, Identifier alias, List<Identifier> columnAliases) {
        return new Impl(rows, columnAliases, alias);
    }

    /**
     * Rows of expressions; all rows must have the same arity.
     *
     * @return row values for the VALUES table
     */
    RowValues values();

    /**
     * Adds column names to the alias.
     * Note: column names without {@link ValuesTable#alias()} are ignored.
     *
     * @param columnAliases a list of column names to add.
     * @return this.
     */
    default ValuesTable columnAliases(String... columnAliases) {
        return columnAliases(Stream.of(columnAliases).map(Identifier::of).toList());
    }

    /**
     * Adds derived column aliases preserving quote metadata.
     * Note: column names without {@link ValuesTable#alias()} are ignored.
     *
     * @param columnAliases a list of column alias identifiers to add.
     * @return this.
     */
    default ValuesTable columnAliases(List<Identifier> columnAliases) {
        return new Impl(values(), Objects.requireNonNull(columnAliases), alias());
    }

    /**
     * Adds alias to the values table.
     *
     * @param alias an alias to add.
     * @return A newly created values table with the provide alias. All other fields are preserved.
     */
    default ValuesTable as(String alias) {
        return as(alias == null ? null : Identifier.of(alias));
    }

    /**
     * Adds alias to the values table preserving quote metadata.
     *
     * @param alias an alias to add.
     * @return a newly created values table with the provided alias. All other fields are preserved.
     */
    default ValuesTable as(Identifier alias) {
        return new Impl(values(), columnAliases(), alias);
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
     * @param alias        table alias or null if none
     */
    record Impl(RowValues values, List<Identifier> columnAliases, Identifier alias) implements ValuesTable {

        /**
         * Creates a values table implementation.
         *
         * @param values                 row values
         * @param columnAliases optional column alias identifiers
         * @param alias        table alias identifier
         */
        public Impl {
            Objects.requireNonNull(values, "values");
            columnAliases = columnAliases == null ? List.of() : List.copyOf(columnAliases);
        }
    }
}
