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
        return of(function, List.of(), null, false);
    }

    /**
     * Creates a function table reference with a table alias.
     *
     * @param function the function call
     * @param alias    the table alias
     * @return function table reference
     */
    static FunctionTable of(FunctionExpr function, String alias) {
        return of(function, List.of(), alias, false);
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
        return of(function, columnAliases, alias, false);
    }

    /**
     * Creates a function table reference with explicit fields.
     *
     * @param function       the function call
     * @param columnAliases  the column aliases
     * @param alias          the table alias
     * @param ordinality whether {@code WITH ORDINALITY} is enabled
     * @return function table reference
     */
    static FunctionTable of(FunctionExpr function, List<String> columnAliases, String alias, boolean ordinality) {
        return new Impl(function, columnAliases, alias, ordinality);
    }

    /**
     * The function call that produces rows for this table reference.
     *
     * @return function call expression
     */
    FunctionExpr function();

    /**
     * Indicates whether {@code WITH ORDINALITY} is enabled.
     *
     * @return true if the function table includes {@code WITH ORDINALITY}
     */
    boolean ordinality();

    /**
     * Adds an alias to a function table.
     *
     * @param alias an alias to add.
     * @return this.
     */
    default FunctionTable as(String alias) {
        return new Impl(function(), columnAliases(), alias, ordinality());
    }

    /**
     * Adds column names to the alias.
     * Note: column names without {@link FunctionTable#alias()} are ignored.
     *
     * @param columnAliases a list of column names to add.
     * @return this.
     */
    default FunctionTable columnAliases(List<String> columnAliases) {
        return new Impl(function(), Objects.requireNonNull(columnAliases), alias(), ordinality());
    }

    /**
     * Adds column names to the alias.
     * Note: column names without {@link FunctionTable#alias()} are ignored.
     *
     * @param columnAliases a list of column names to add.
     * @return this.
     */
    default FunctionTable columnAliases(String... columnAliases) {
        return new Impl(function(), List.of(columnAliases), alias(), ordinality());
    }

    /**
     * Enables {@code WITH ORDINALITY} for this function table.
     *
     * @return a new function table with {@code WITH ORDINALITY} enabled
     */
    default FunctionTable withOrdinality() {
        return new Impl(function(), columnAliases(), alias(), true);
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
     *
     * @param function       the function call
     * @param columnAliases  a list of column names to add.
     * @param alias          an alias to add.
     * @param ordinality whether {@code WITH ORDINALITY} is enabled
     */
    record Impl(FunctionExpr function, List<String> columnAliases, String alias, boolean ordinality) implements FunctionTable {

        /**
         * Creates a function table implementation.
         *
         * @param function      the function call
         * @param columnAliases column aliases
         * @param alias         table alias
         * @param ordinality    whether {@code WITH ORDINALITY} is enabled
         */
        public Impl {
            Objects.requireNonNull(function, "function");
            columnAliases = columnAliases == null ? null : List.copyOf(columnAliases);
        }
    }
}
