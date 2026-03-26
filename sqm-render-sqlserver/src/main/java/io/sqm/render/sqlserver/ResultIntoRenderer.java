package io.sqm.render.sqlserver;

import io.sqm.core.ResultInto;
import io.sqm.core.Table;
import io.sqm.core.VariableTableRef;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders SQL Server-style {@code OUTPUT ... INTO ...} targets.
 */
public class ResultIntoRenderer implements Renderer<ResultInto> {

    /**
     * Creates a result-into renderer.
     */
    public ResultIntoRenderer() {
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(ResultInto node, RenderContext ctx, SqlWriter w) {
        if (!(node.target() instanceof Table || node.target() instanceof VariableTableRef)) {
            throw new UnsupportedOperationException("SQL Server OUTPUT INTO currently supports base tables and table variables only");
        }

        if (node.target() instanceof Table target
            && target.hints().stream().anyMatch(h -> switch (h.name().value()) {
                case "NOLOCK", "UPDLOCK", "HOLDLOCK" -> true;
                default -> false;
            })) {
            throw new UnsupportedOperationException("SQL Server table hints are not supported on OUTPUT INTO targets");
        }

        w.append("INTO").space().append(node.target());
        if (!node.columns().isEmpty()) {
            w.space().append("(");
            w.comma(node.columns(), ctx.dialect().quoter());
            w.append(")");
        }
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<ResultInto> targetType() {
        return ResultInto.class;
    }
}
