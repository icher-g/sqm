package io.sqm.core.walk;

import io.sqm.core.*;

/**
 * Visitor for {@link ResultItem} nodes.
 * <p>
 * This visitor is used to traverse items that appear inside a {@link ResultClause}.
 * A result item represents a single element produced by a data-modifying statement
 * such as {@code INSERT}, {@code UPDATE}, or {@code DELETE}.
 * <p>
 * Depending on the SQL dialect this clause may be rendered as:
 * <ul>
 *     <li>{@code RETURNING} in PostgreSQL and MySQL</li>
 *     <li>{@code OUTPUT} in T-SQL</li>
 * </ul>
 * <p>
 * Implementations of this visitor typically perform tasks such as SQL rendering,
 * validation, or transformation of result items.
 *
 * @param <R> return type of visitor methods
 */
public interface ResultItemVisitor<R> {

    /**
     * Visits an expression-based result item.
     * <p>
     * This item represents a returned expression optionally aliased with an identifier.
     * Examples:
     * <pre>
     * RETURNING id
     * RETURNING upper(name) AS normalized
     * OUTPUT INSERTED.id
     * </pre>
     *
     * @param i expression result item
     * @return visitor result
     */
    R visitExprResultItem(ExprResultItem i);

    /**
     * Visits a star result item.
     * <p>
     * Represents returning all columns of the affected row.
     * Examples:
     * <pre>
     * RETURNING *
     * OUTPUT *
     * </pre>
     *
     * @param i star result item
     * @return visitor result
     */
    R visitStarResultItem(StarResultItem i);

    /**
     * Visits a qualified star result item.
     * <p>
     * Represents returning all columns from a qualified row source.
     * Examples:
     * <pre>
     * RETURNING t.*
     * </pre>
     *
     * @param i qualified star result item
     * @return visitor result
     */
    R visitQualifiedStarResultItem(QualifiedStarResultItem i);

    /**
     * Visits a SQL Server output star result item.
     * <p>
     * Represents returning all columns from a SQL Server pseudo-row source.
     * Examples:
     * <pre>
     * OUTPUT INSERTED.*
     * OUTPUT DELETED.*
     * </pre>
     *
     * @param i output-star result item
     * @return visitor result
     */
    R visitOutputStarResultItem(OutputStarResultItem i);
}
