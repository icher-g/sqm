package io.sqm.render.ansi;

import io.sqm.core.LockMode;
import io.sqm.core.LockingClause;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

public class LockingClauseRenderer implements Renderer<LockingClause> {
    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(LockingClause node, RenderContext ctx, SqlWriter w) {
        if (node.mode() != LockMode.UPDATE) {
            throw new UnsupportedDialectFeatureException(node.mode().toString(), "ANSI");
        }

        if (!node.ofTables().isEmpty()) {
            throw new UnsupportedDialectFeatureException("OF t1, t2", "ANSI");
        }

        if (node.nowait()) {
            throw new UnsupportedDialectFeatureException("NOWAIT", "ANSI");
        }

        if (node.skipLocked()) {
            throw new UnsupportedDialectFeatureException("SKIP LOCKED", "ANSI");
        }

        w.append("FOR UPDATE");
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<? extends LockingClause> targetType() {
        return LockingClause.class;
    }
}
