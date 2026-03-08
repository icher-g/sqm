package io.sqm.render;

import io.sqm.core.Identifier;
import io.sqm.core.Node;
import io.sqm.render.spi.IdentifierQuoter;
import io.sqm.render.spi.Renderer;

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
     * Appends a node to the query. The node will be rendered with the {@link Renderer} interface.
     *
     * @param node a node to append.
     * @param <T>  the type of the Entity.
     * @return this.
     */
    <T extends Node> SqlWriter append(T node);

    /**
     * Appends a node using an explicit target type renderer.
     *
     * @param type explicit renderer type key
     * @param node node instance to render
     * @param <T> node type
     * @return this.
     */
    default <T extends Node> SqlWriter append(Class<T> type, T node) {
        return append(node);
    }

    /**
     * Appends a node to the query inside the parentheses. The node will be rendered with the {@link Renderer} interface.
     *
     * @param node    a node to append.
     * @param enclose indicates that the node needs to be wrapped with the parenthesis.
     * @param <T>     the type of the Entity.
     * @return this.
     */
    default <T extends Node> SqlWriter append(T node, boolean enclose) {
        return append(node, enclose, false);
    }

    /**
     * Appends a node to the query inside the parentheses. The node will be rendered with the {@link Renderer} interface.
     *
     * @param node      a node to append.
     * @param enclose   indicates that the node needs to be wrapped with the parenthesis.
     * @param multiline indicates if the new line needs to be added after and before the parenthesis.
     * @param <T>       the type of the Entity.
     * @return this.
     */
    default <T extends Node> SqlWriter append(T node, boolean enclose, boolean multiline) {
        if (enclose) {
            append("(");
            if (multiline) {
                newline().indent();
            }
        }

        append(node);

        if (enclose) {
            if (multiline) {
                outdent().newline();
            }
            append(")");
        }
        return this;
    }

    /**
     * Appends a list of entities rendered with the {@link Renderer} separated by comma.
     *
     * @param parts a list of entities to append.
     * @param <T>   the entity type.
     * @return this.
     */
    default <T extends Node> SqlWriter comma(List<T> parts) {
        return comma(parts, false);
    }

    /**
     * Appends a list of entities rendered with the {@link Renderer} separated by comma.
     *
     * @param parts   a list of entities to append.
     * @param enclose indicates that each part needs to be wrapped with the parenthesis.
     * @param <T>     the entity type.
     * @return this.
     */
    default <T extends Node> SqlWriter comma(List<T> parts, boolean enclose) {
        if (parts == null || parts.isEmpty()) return this;
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                append(",");
                append(" ");
            }
            append(parts.get(i), enclose);
        }
        return this;
    }

    /**
     * Appends a list of identifiers separated by comma.
     *
     * @param parts a list of identifiers to append.
     * @param quoter dialect-specific identifier quoter.
     * @return this.
     */
    default SqlWriter comma(List<Identifier> parts, IdentifierQuoter quoter) {
        if (parts == null || parts.isEmpty()) return this;
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                append(",").space();
            }
            Identifier identifier = parts.get(i);
            if (identifier.quoted()) {
                if (quoter.supports(identifier.quoteStyle())) {
                    append(quoter.quote(identifier.value(), identifier.quoteStyle()));
                }
                else {
                    append(quoter.quote(identifier.value()));
                }
            }
            else {
                append(quoter.quoteIfNeeded(identifier.value()));
            }
        }
        return this;
    }

    /**
     * Indicates whether the call to {@link SqlWriter#newline()} should be ignored and new line should not be added.
     */
    void singleLine();

    /**
     * Indicates whether the call to {@link SqlWriter#newline()} should add a new line to the writer.
     */
    void multiLine();

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
    @SuppressWarnings("UnusedReturnValue")
    SqlWriter indent();

    /**
     * Decreases the current indent used by the writer.
     *
     * @return this.
     */
    SqlWriter outdent();

    /**
     * Gets a written SQL string together with the list of params if there are any.
     *
     * @param params a list of params to add to the result.
     * @return an {@link SqlText}.
     */
    SqlText toText(List<Object> params);
}

