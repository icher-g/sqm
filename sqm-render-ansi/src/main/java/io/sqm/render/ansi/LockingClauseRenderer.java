package io.sqm.render.ansi;

import io.sqm.core.LockingClause;
import io.sqm.core.dialect.SqlFeature;
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
        w.append("FOR").space();

        switch (node.mode()) {
            case UPDATE -> {
                if (!ctx.dialect().capabilities().supports(SqlFeature.LOCKING_CLAUSE)) {
                    throw new UnsupportedDialectFeatureException("FOR UPDATE", ctx.dialect().name());
                }
                w.append("UPDATE");
            }
            case NO_KEY_UPDATE -> {
                if (!ctx.dialect().capabilities().supports(SqlFeature.LOCKING_NO_KEY_UPDATE)) {
                    throw new UnsupportedDialectFeatureException("FOR NO KEY UPDATE", ctx.dialect().name());
                }
                w.append("NO KEY UPDATE");
            }
            case SHARE -> {
                if (!ctx.dialect().capabilities().supports(SqlFeature.LOCKING_SHARE)) {
                    throw new UnsupportedDialectFeatureException("FOR SHARE", ctx.dialect().name());
                }
                w.append("SHARE");
            }
            case KEY_SHARE -> {
                if (!ctx.dialect().capabilities().supports(SqlFeature.LOCKING_KEY_SHARE)) {
                    throw new UnsupportedDialectFeatureException("FOR KEY SHARE", ctx.dialect().name());
                }
                w.append("KEY SHARE");
            }
            default -> throw new IllegalStateException("Unexpected value: " + node.mode());
        }

        if (!node.ofTables().isEmpty()) {
            if (!ctx.dialect().capabilities().supports(SqlFeature.LOCKING_OF)) {
                throw new UnsupportedDialectFeatureException("FOR UPDATE OF", ctx.dialect().name());
            }
            var quoter = ctx.dialect().quoter();
            w.space().append("OF").space();
            for (int i = 0; i < node.ofTables().size(); i++) {
                if (i > 0) {
                    w.append(",").space();
                }
                w.append(renderIdentifier(node.ofTables().get(i).identifier(), quoter));
            }
        }

        if (node.nowait()) {
            if (!ctx.dialect().capabilities().supports(SqlFeature.LOCKING_NOWAIT)) {
                throw new UnsupportedDialectFeatureException("NOWAIT", ctx.dialect().name());
            }
            w.space().append("NOWAIT");
        }

        if (node.skipLocked()) {
            if (!ctx.dialect().capabilities().supports(SqlFeature.LOCKING_SKIP_LOCKED)) {
                throw new UnsupportedDialectFeatureException("SKIP LOCKED", ctx.dialect().name());
            }
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
