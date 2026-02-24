package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * A subquery used as a table source: (SELECT ...) AS alias
 */
public non-sealed interface QueryTable extends AliasedTableRef {

    /**
     * Wraps a query as a table for use in FROM statement.
     *
     * @param query a query to wrap.
     * @return A newly created instance of a wrapped query.
     */
    static QueryTable of(Query query) {
        return new Impl(query, List.of(), null);
    }

    /**
     * Wraps a query as a table for use in FROM statement.
     *
     * @param query         a query to wrap.
     * @param alias         an alias identifier.
     * @param columnAliases derived column aliases.
     * @return a newly created wrapped query table.
     */
    static QueryTable of(Query query, Identifier alias, List<Identifier> columnAliases) {
        return new Impl(query, columnAliases, alias);
    }

    /**
     * Gets a sub query.
     *
     * @return a sub query.
     */
    Query query();

    /**
     * Adds column names to the alias.
     * Note: column names without {@link QueryTable#alias()} are ignored.
     *
     * @param columnAliases a list of column names to add.
     * @return this.
     */
    default QueryTable columnAliases(String... columnAliases) {
        return columnAliases(Stream.of(columnAliases).map(Identifier::of).toList());
    }

    /**
     * Adds derived column aliases preserving quote metadata.
     * Note: column names without {@link QueryTable#alias()} are ignored.
     *
     * @param columnAliases a list of column alias identifiers to add.
     * @return this.
     */
    default QueryTable columnAliases(List<Identifier> columnAliases) {
        return new Impl(query(), columnAliases, alias());
    }

    /**
     * Adds an alias to a query table.
     *
     * @param alias an alias to add.
     * @return this.
     */
    default QueryTable as(String alias) {
        return as(alias == null ? null : Identifier.of(alias));
    }

    /**
     * Adds an alias to a query table preserving quote metadata.
     *
     * @param alias an alias identifier to add.
     * @return this.
     */
    default QueryTable as(Identifier alias) {
        return new Impl(query(), columnAliases(), alias);
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
        return v.visitQueryTable(this);
    }

    /**
     * Implements a query table. A wrapper for a query to be used in FROM statement.
     *
     * @param query                  a query to wrap.
     * @param columnAliases Optional derived column list; may be null or empty.
     * @param alias        an alias of the table.
     */
    record Impl(Query query, List<Identifier> columnAliases, Identifier alias) implements QueryTable {
        /**
         * Creates a query table implementation.
         *
         * @param query                  a query to wrap
         * @param columnAliases optional derived column aliases
         * @param alias        table alias identifier
         */
        public Impl {
            Objects.requireNonNull(query, "query");
            columnAliases = columnAliases == null ? List.of() : List.copyOf(columnAliases);
        }
    }
}
