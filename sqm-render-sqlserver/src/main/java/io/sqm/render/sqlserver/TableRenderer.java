package io.sqm.render.sqlserver;

import io.sqm.core.Table;
import io.sqm.core.TableHint;
import io.sqm.core.TableVersionSpec;
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
    protected void renderTableVersion(Table node, RenderContext ctx, SqlWriter w) {
        TableVersionSpec version = node.version();
        if (version == null) {
            return;
        }
        w.space().append("FOR SYSTEM_TIME");
        switch (version.kind()) {
            case AS_OF_TIMESTAMP -> w.space().append("AS OF").space().append(version.value());
            case FROM_TO -> w.space().append("FROM").space().append(version.start()).space().append("TO").space().append(version.end());
            case BETWEEN -> w.space().append("BETWEEN").space().append(version.start()).space().append("AND").space().append(version.end());
            case CONTAINED_IN -> w.space().append("CONTAINED IN").space().append("(").append(version.start()).append(", ").append(version.end()).append(")");
            case ALL -> w.space().append("ALL");
            default -> super.renderTableVersion(node, ctx, w);
        }
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
