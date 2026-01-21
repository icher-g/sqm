package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.List;
import java.util.Objects;

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
    default QueryTable columnAliases(List<String> columnAliases) {
        return new Impl(query(), Objects.requireNonNull(columnAliases), alias());
    }

    /**
     * Adds column names to the alias.
     * Note: column names without {@link QueryTable#alias()} are ignored.
     *
     * @param columnAliases a list of column names to add.
     * @return this.
     */
    default QueryTable columnAliases(String... columnAliases) {
        return new Impl(query(), List.of(columnAliases), alias());
    }

    /**
     * Adds an alias to a query table.
     *
     * @param alias an alias to add.
     * @return this.
     */
    default QueryTable as(String alias) {
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
     * @param query         a query to wrap.
     * @param columnAliases Optional derived column list; may be null or empty.
     * @param alias         an alias of the table.
     */
    record Impl(Query query, List<String> columnAliases, String alias) implements QueryTable {
    }
}
