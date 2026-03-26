package io.sqm.render.sqlserver;

import io.sqm.core.Table;
import io.sqm.core.TableHint;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

import java.util.HashSet;

/**
 * Renders SQL Server table references with {@code WITH (...)} table hints.
 */
public class TableRenderer extends io.sqm.render.ansi.TableRenderer {

    /**
     * Creates a SQL Server table renderer.
     */
    public TableRenderer() {
    }

    @Override
    protected void renderTableHints(Table node, RenderContext ctx, SqlWriter w) {
        var hints = node.hints().stream().filter(TableRenderer::isSqlServerLockHint).toList();
        if (hints.isEmpty()) {
            if (!node.hints().isEmpty()) {
                throw new UnsupportedDialectFeatureException("table hints", ctx.dialect().name());
            }
            return;
        }

        if (hints.size() != node.hints().size()) {
            throw new UnsupportedDialectFeatureException("table hints", ctx.dialect().name());
        }

        if (!ctx.dialect().capabilities().supports(SqlFeature.TABLE_LOCK_HINT)) {
            throw new UnsupportedDialectFeatureException("SQL Server table hints", ctx.dialect().name());
        }

        validateHints(hints);

        w.space().append("WITH").space().append("(");
        for (int i = 0; i < hints.size(); i++) {
            if (i > 0) {
                w.append(", ");
            }
            w.append(hints.get(i).name().value());
        }
        w.append(")");
    }

    private void validateHints(java.util.List<TableHint> hints) {
        var seen = new HashSet<String>();
        for (var hint : hints) {
            if (!seen.add(hint.name().value())) {
                throw new UnsupportedOperationException("Duplicate SQL Server table hint " + hint.name().value());
            }
        }
        if (seen.contains("NOLOCK")
            && (seen.contains("UPDLOCK") || seen.contains("HOLDLOCK"))) {
            throw new UnsupportedOperationException("SQL Server NOLOCK cannot be combined with UPDLOCK or HOLDLOCK");
        }
    }

    private static boolean isSqlServerLockHint(TableHint hint) {
        return switch (hint.name().value()) {
            case "NOLOCK", "UPDLOCK", "HOLDLOCK" -> true;
            default -> false;
        };
    }
}
