package io.sqm.render.spi;

import io.sqm.core.AliasedTableRef;
import io.sqm.core.Identifier;
import io.sqm.core.Node;
import io.sqm.core.QualifiedName;
import io.sqm.core.repos.Handler;
import io.sqm.render.SqlWriter;

import java.util.List;

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
        renderTableAlias(node.alias(), node.columnAliases(), ctx, w);
    }

    /**
     * Renders a table-reference alias and optional derived column aliases.
     *
     * @param alias table-reference alias, or {@code null}
     * @param columnAliases optional derived column aliases
     * @param ctx render context
     * @param w SQL writer
     */
    default void renderTableAlias(Identifier alias, List<Identifier> columnAliases, RenderContext ctx, SqlWriter w) {
        if (alias != null) {
            var quoter = ctx.dialect().quoter();
            w.space();
            if (ctx.dialect().usesAsForTableAliases()) {
                w.append("AS").space();
            }
            w.append(renderIdentifier(alias, quoter));
            if (columnAliases != null && !columnAliases.isEmpty()) {
                w.append("(");
                w.comma(columnAliases, quoter);
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

    /**
     * Renders a qualified name preserving quote metadata on each identifier part.
     *
     * @param qualifiedName qualified name to render.
     * @param quoter dialect identifier quoter.
     * @return a rendered qualified name.
     */
    default String renderQualifiedName(QualifiedName qualifiedName, IdentifierQuoter quoter) {
        return qualifiedName.parts().stream()
            .map(part -> renderIdentifier(part, quoter))
            .collect(java.util.stream.Collectors.joining("."));
    }
}
