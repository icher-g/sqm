package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.List;
import java.util.Objects;

/**
 * A table reference whose source is a function call.
 * <p>
 * Some SQL dialects allow set-returning functions to appear in the {@code FROM} clause,
 * producing a virtual table. This node models such constructs, for example:
 * <pre>
 * FROM f(1) t
 * FROM f(x) AS t(c1, c2)
 * </pre>
 * <p>
 * If the function needs access to columns from preceding FROM items, wrap this node
 * with {@link Lateral} (for example via {@code .lateral()} on {@link TableRef}).
 */
public non-sealed interface FunctionTable extends AliasedTableRef {

    /**
     * Creates a function table reference without an alias.
     *
     * @param function the function call
     * @return function table reference
     */
    static FunctionTable of(FunctionExpr function) {
        return new Impl(function, List.of(), null);
    }

    /**
     * Creates a function table reference with a table alias.
     *
     * @param function the function call
     * @param alias    the table alias
     * @return function table reference
     */
    static FunctionTable of(FunctionExpr function, String alias) {
        return new Impl(function, List.of(), alias);
    }

    /**
     * Creates a function table reference with a table alias and column aliases.
     *
     * @param function      the function call
     * @param columnAliases the column aliases
     * @param alias         the table alias
     * @return function table reference
     */
    static FunctionTable of(FunctionExpr function, List<String> columnAliases, String alias) {
        return new Impl(function, columnAliases, alias);
    }

    /**
     * The function call that produces rows for this table reference.
     */
    FunctionExpr function();

    /**
     * Adds an alias to a function table.
     *
     * @param alias an alias to add.
     * @return this.
     */
    default FunctionTable as(String alias) {
        return new Impl(function(), columnAliases(), alias);
    }

    /**
     * Adds column names to the alias.
     * Note: column names without {@link FunctionTable#alias()} are ignored.
     *
     * @param columnAliases a list of column names to add.
     * @return this.
     */
    default FunctionTable columnAliases(List<String> columnAliases) {
        return new Impl(function(), Objects.requireNonNull(columnAliases), alias());
    }

    /**
     * Adds column names to the alias.
     * Note: column names without {@link FunctionTable#alias()} are ignored.
     *
     * @param columnAliases a list of column names to add.
     * @return this.
     */
    default FunctionTable columnAliases(String... columnAliases) {
        return new Impl(function(), List.of(columnAliases), alias());
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
        return v.visitFunctionTable(this);
    }

    /**
     * Default implementation.
     */
    record Impl(FunctionExpr function, List<String> columnAliases, String alias) implements FunctionTable {

        public Impl {
            Objects.requireNonNull(function, "function");
            columnAliases = columnAliases == null ? List.of() : List.copyOf(columnAliases);
        }
    }
}
