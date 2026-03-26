package io.sqm.render.mysql;

import io.sqm.core.SelectModifier;
import io.sqm.core.SelectQuery;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders MySQL SELECT query statements.
 */
public class SelectQueryRenderer extends io.sqm.render.ansi.SelectQueryRenderer {

    /**
     * Creates a MySQL SELECT-query renderer.
     */
    public SelectQueryRenderer() {
    }

    /**
     * Renders MySQL-specific tokens after {@code SELECT}.
     *
     * @param node query node.
     * @param ctx  render context.
     * @param w    sql writer.
     */
    @Override
    protected void renderAfterSelectKeyword(SelectQuery node, RenderContext ctx, SqlWriter w) {
        MySqlHintRenderSupport.renderStatementHints(node.hints(), "SELECT optimizer hints", ctx, w);

        for (var modifier : node.modifiers()) {
            if (modifier == SelectModifier.CALC_FOUND_ROWS) {
                w.space().append("SQL_CALC_FOUND_ROWS");
            }
        }
    }
}
