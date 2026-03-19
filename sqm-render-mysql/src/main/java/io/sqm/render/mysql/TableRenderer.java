package io.sqm.render.mysql;

import io.sqm.core.Table;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders MySQL table references with index hints.
 */
public class TableRenderer extends io.sqm.render.ansi.TableRenderer {

    /**
     * Creates a MySQL table renderer.
     */
    public TableRenderer() {
    }

    /**
     * Renders table references including index hints.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(Table node, RenderContext ctx, SqlWriter w) {
        super.render(node, ctx, w);

        if (node.indexHints().isEmpty()) {
            return;
        }

        if (!ctx.dialect().capabilities().supports(SqlFeature.TABLE_INDEX_HINT)) {
            throw new UnsupportedDialectFeatureException("table index hints", ctx.dialect().name());
        }

        var quoter = ctx.dialect().quoter();
        for (var hint : node.indexHints()) {
            w.space().append(hint.type().name()).space().append("INDEX");
            if (hint.scope() != Table.IndexHintScope.DEFAULT) {
                w.space().append("FOR").space();
                switch (hint.scope()) {
                    case JOIN -> w.append("JOIN");
                    case ORDER_BY -> w.append("ORDER BY");
                    case GROUP_BY -> w.append("GROUP BY");
                    default -> {
                        // no-op
                    }
                }
            }

            w.space().append("(");
            for (int i = 0; i < hint.indexes().size(); i++) {
                if (i > 0) {
                    w.append(", ");
                }
                w.append(renderIdentifier(hint.indexes().get(i), quoter));
            }
            w.append(")");
        }
    }
}
