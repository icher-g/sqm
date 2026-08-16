package io.sqm.render.ansi;

import io.sqm.core.Table;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders table references.
 */
public class TableRenderer implements Renderer<Table> {
    /**
     * Creates a table renderer.
     */
    public TableRenderer() {
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(Table node, RenderContext ctx, SqlWriter w) {
        var quoter = ctx.dialect().quoter();
        var schema = node.schema();
        if (node.inheritance() == Table.Inheritance.ONLY) {
            if (!ctx.dialect().capabilities().supports(SqlFeature.TABLE_INHERITANCE_ONLY)) {
                throw new UnsupportedDialectFeatureException("ONLY", ctx.dialect().name());
            }
            w.append("ONLY").space();
        }
        if (schema != null) {
            w.append(renderIdentifier(schema, quoter));
            w.append(".");
        }
        w.append(renderIdentifier(node.name(), quoter));
        renderTableVersion(node, ctx, w);
        renderPartitionSpec(node, ctx, w);
        if (node.inheritance() == Table.Inheritance.INCLUDE_DESCENDANTS) {
            if (!ctx.dialect().capabilities().supports(SqlFeature.TABLE_INHERITANCE_DESCENDANTS)) {
                throw new UnsupportedDialectFeatureException("table *", ctx.dialect().name());
            }
            w.space().append("*");
        }

        renderTableAlias(node.alias(), null, ctx, w);

        renderTableHints(node, ctx, w);
    }

    /**
     * Renders optional table-attached hints.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    protected void renderTableHints(Table node, RenderContext ctx, SqlWriter w) {
        if (!node.hints().isEmpty()) {
            throw new UnsupportedDialectFeatureException("table hints", ctx.dialect().name());
        }
    }

    /**
     * Renders optional table version selector.
     *
     * @param node table node
     * @param ctx render context
     * @param w SQL writer
     */
    protected void renderTableVersion(Table node, RenderContext ctx, SqlWriter w) {
        if (node.version() != null) {
            throw new UnsupportedDialectFeatureException("table versioning", ctx.dialect().name());
        }
    }

    /**
     * Renders optional table partition selector.
     *
     * @param node table node
     * @param ctx render context
     * @param w SQL writer
     */
    protected void renderPartitionSpec(Table node, RenderContext ctx, SqlWriter w) {
        if (node.partitionSpec() != null) {
            throw new UnsupportedDialectFeatureException("table partition selector", ctx.dialect().name());
        }
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<Table> targetType() {
        return Table.class;
    }
}
