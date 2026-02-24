package io.sqm.core;

import java.util.List;

/**
 * A {@link TableRef} that can define a table alias and an optional derived column list.
 * <p>
 * This interface models table-producing expressions whose result columns are not fixed
 * by a catalog definition and therefore may be renamed in the {@code FROM} clause.
 * Typical examples include:
 * <ul>
 *   <li>Derived tables (subqueries)</li>
 *   <li>{@code VALUES} tables</li>
 *   <li>Set-returning / table-valued functions</li>
 * </ul>
 * <p>
 * The derived column list, when present, assigns names to the columns produced by
 * the table reference and is written in SQL as:
 * <pre>
 * FROM &lt;table_expr&gt; [AS] alias (col1, col2, ...)
 * </pre>
 * <p>
 * Base tables do not support derived column lists and therefore do not implement
 * this interface.
 */
public sealed interface AliasedTableRef extends TableRef permits FunctionTable, QueryTable, ValuesTable {

    /**
     * Returns the table alias identifier with quote metadata preserved.
     *
     * @return the alias identifier, or {@code null} if no alias is defined
     */
    Identifier alias();

    /**
     * Returns the derived column list for this table reference preserving quote metadata.
     * <p>
     * The list is empty when no column aliases are defined.
     * Column aliases are positional and correspond to the columns
     * produced by the underlying table expression.
     *
     * @return an immutable list of column alias identifiers
     */
    List<Identifier> columnAliases();
}

