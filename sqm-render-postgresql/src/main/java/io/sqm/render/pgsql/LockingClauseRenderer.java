package io.sqm.render.pgsql;

import io.sqm.core.LockingClause;
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
        w.append("FOR").space();

        switch (node.mode()) {
            case UPDATE -> w.append("UPDATE");
            case NO_KEY_UPDATE -> w.append("NO KEY UPDATE");
            case SHARE -> w.append("SHARE");
            case KEY_SHARE -> w.append("KEY SHARE");
            default -> throw new IllegalStateException("Unexpected value: " + node.mode());
        }

        if (!node.ofTables().isEmpty()) {
            w.space().append("OF").space();
            for (int i = 0; i < node.ofTables().size(); i++) {
                if (i > 0) {
                    w.append(",").space();
                }
                w.append(node.ofTables().get(i).identifier());
            }
        }

        if (node.nowait()) {
            w.space().append("NOWAIT");
        }

        if (node.skipLocked()) {
            w.space().append("SKIP LOCKED");
        }
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
