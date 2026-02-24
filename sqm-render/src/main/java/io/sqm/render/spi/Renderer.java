package io.sqm.render.spi;

import io.sqm.core.AliasedTableRef;
import io.sqm.core.Identifier;
import io.sqm.core.Node;
import io.sqm.core.repos.Handler;
import io.sqm.render.SqlWriter;

/**
 * A base interface for all renderers.
 *
 * @param <T> the type of the node to render.
 */
public interface Renderer<T extends Node> extends Handler<T> {
    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    void render(T node, RenderContext ctx, SqlWriter w);

    /**
     * Renders {@link AliasedTableRef} node that has an alias ana list of column aliases.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    default void renderAliased(AliasedTableRef node, RenderContext ctx, SqlWriter w) {
        var alias = node.alias();
        if (alias != null) {
            var quoter = ctx.dialect().quoter();
            w.space().append("AS").space().append(renderIdentifier(alias, quoter));
            var columnAliases = node.columnAliases();
            if (columnAliases != null && !columnAliases.isEmpty()) {
                w.append("(");
                w.append(String.join(
                    ", ",
                    columnAliases.stream().map(a -> renderIdentifier(a, quoter)).toList()));
                w.append(")");
            }
        }
    }

    /**
     * Renders an identifier preserving the original quote style when supported by the target dialect.
     * If the original style is unsupported, the dialect default quoting is used.
     *
     * @param identifier an identifier to render.
     * @param quoter     dialect identifier quoter.
     * @return a rendered identifier.
     */
    default String renderIdentifier(Identifier identifier, IdentifierQuoter quoter) {
        if (identifier.quoted()) {
            if (quoter.supports(identifier.quoteStyle())) {
                return quoter.quote(identifier.value(), identifier.quoteStyle());
            }
            return quoter.quote(identifier.value());
        }
        return quoter.quoteIfNeeded(identifier.value());
    }
}
