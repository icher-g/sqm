package io.sqm.render.ansi;

import io.sqm.core.MergeInsertAction;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Baseline ANSI renderer for {@link MergeInsertAction}.
 */
public class MergeInsertActionRenderer implements Renderer<MergeInsertAction> {

    /**
     * Creates a merge-insert-action renderer.
     */
    public MergeInsertActionRenderer() {
    }

    @Override
    public void render(MergeInsertAction node, RenderContext ctx, SqlWriter w) {
        throw new UnsupportedDialectFeatureException("MERGE INSERT action", ctx.dialect().name());
    }

    /**
     * Renders the shared MERGE insert-action subset used by dialect-specific implementations.
     *
     * @param node merge insert action to render
     * @param ctx render context
     * @param w SQL writer
     */
    protected final void renderSupportedAction(MergeInsertAction node, RenderContext ctx, SqlWriter w) {
        w.append("INSERT");
        if (!node.columns().isEmpty()) {
            w.space().append("(");
            for (int i = 0; i < node.columns().size(); i++) {
                if (i > 0) {
                    w.append(", ");
                }
                w.append(renderIdentifier(node.columns().get(i), ctx.dialect().quoter()));
            }
            w.append(")");
        }
        w.space().append("VALUES").space().append("(");
        for (int i = 0; i < node.values().items().size(); i++) {
            if (i > 0) {
                w.append(", ");
            }
            w.append(node.values().items().get(i));
        }
        w.append(")");
    }

    @Override
    public Class<MergeInsertAction> targetType() {
        return MergeInsertAction.class;
    }
}
