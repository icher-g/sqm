package io.sqm.render.mysql;

import io.sqm.core.Table;
import io.sqm.core.TableHint;
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

    @Override
    protected void renderTableHints(Table node, RenderContext ctx, SqlWriter w) {
        var indexHints = node.hints().stream().filter(MySqlHintRenderSupport::isIndexHint).toList();
        if (indexHints.isEmpty()) {
            if (!node.hints().isEmpty()) {
                throw new UnsupportedDialectFeatureException("table hints", ctx.dialect().name());
            }
            return;
        }

        if (indexHints.size() != node.hints().size()) {
            throw new UnsupportedDialectFeatureException("table hints", ctx.dialect().name());
        }

        if (!ctx.dialect().capabilities().supports(SqlFeature.TABLE_INDEX_HINT)) {
            throw new UnsupportedDialectFeatureException("table index hints", ctx.dialect().name());
        }

        for (var hint : indexHints) {
            renderIndexHint(hint, ctx, w);
        }
    }

    private void renderIndexHint(TableHint hint, RenderContext ctx, SqlWriter w) {
        w.space().append(MySqlHintRenderSupport.indexHintKeyword(hint)).space().append("INDEX");
        var scope = MySqlHintRenderSupport.indexHintScope(hint);
        if (scope != null) {
                w.space().append("FOR").space();
            w.append(scope);
        }
        w.space().append("(");
        MySqlHintRenderSupport.renderHintArgs(hint, ctx, w);
        w.append(")");
    }
}
