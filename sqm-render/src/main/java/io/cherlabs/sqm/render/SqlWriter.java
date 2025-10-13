package io.cherlabs.sqm.render;

import io.cherlabs.sqm.core.Entity;

import java.util.List;

/**
 * An interface for SQL query writing.
 */
public interface SqlWriter {
    /**
     * Appends a string to the query.
     *
     * @param s a string to append.
     * @return this.
     */
    SqlWriter append(String s);

    /**
     * Appends an entity to the query. The entity will be rendered with the {@link Renderer} interface.
     *
     * @param entity an entity to append.
     * @param <T>    the type of the Entity.
     * @return this.
     */
    <T extends Entity> SqlWriter append(T entity);

    /**
     * Indicates whether the call to {@link SqlWriter#newline()} should be ignored and new line should not be added.
     *
     * @param ignore True to ignore appending new lines and False to revert the change.
     */
    void ignoreNewLine(boolean ignore);

    /**
     * Appends a space to the query.
     *
     * @return this.
     */
    SqlWriter space();

    /**
     * Appends new line to the query if ignore new line is set to false.
     *
     * @return this.
     */
    SqlWriter newline();

    /**
     * Increases the current indent used by the writer.
     *
     * @return this.
     */
    SqlWriter indent();

    /**
     * Decreases the current indent used by the writer.
     *
     * @return this.
     */
    SqlWriter outdent();

    /**
     * Appends a list of entities rendered with the {@link Renderer} separated by comma.
     *
     * @param parts a list of entities to append.
     * @param <T>   the entity type.
     * @return this.
     */
    <T extends Entity> SqlWriter comma(List<T> parts);

    /**
     * Gets a written SQL string together with the list of parameters if there are any.
     *
     * @param params a list of parameters to add to the result.
     * @return an {@link SqlText}.
     */
    SqlText toText(List<Object> params);
}

