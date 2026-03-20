package io.sqm.render.sqlserver;

import io.sqm.core.Table;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

import java.util.EnumSet;

/**
 * Renders SQL Server table references with {@code WITH (...)} table hints.
 */
public class TableRenderer extends io.sqm.render.ansi.TableRenderer {

    /**
     * Creates a SQL Server table renderer.
     */
    public TableRenderer() {
    }

    /**
     * Renders table references including SQL Server table hints.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(Table node, RenderContext ctx, SqlWriter w) {
        super.render(node, ctx, w);

        if (node.lockHints().isEmpty()) {
            return;
        }

        if (!ctx.dialect().capabilities().supports(SqlFeature.TABLE_LOCK_HINT)) {
            throw new UnsupportedDialectFeatureException("SQL Server table hints", ctx.dialect().name());
        }

        validateHints(node);

        w.space().append("WITH").space().append("(");
        for (int i = 0; i < node.lockHints().size(); i++) {
            if (i > 0) {
                w.append(", ");
            }
            w.append(node.lockHints().get(i).kind().name());
        }
        w.append(")");
    }

    private void validateHints(Table node) {
        var seen = EnumSet.noneOf(Table.LockHintKind.class);
        for (var hint : node.lockHints()) {
            if (!seen.add(hint.kind())) {
                throw new UnsupportedOperationException("Duplicate SQL Server table hint " + hint.kind().name());
            }
        }
        if (seen.contains(Table.LockHintKind.NOLOCK)
            && (seen.contains(Table.LockHintKind.UPDLOCK) || seen.contains(Table.LockHintKind.HOLDLOCK))) {
            throw new UnsupportedOperationException("SQL Server NOLOCK cannot be combined with UPDLOCK or HOLDLOCK");
        }
    }
}
