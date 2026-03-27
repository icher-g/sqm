package io.sqm.render.sqlserver;

import io.sqm.core.JoinKind;
import io.sqm.core.Lateral;
import io.sqm.core.LiteralExpr;
import io.sqm.core.OnJoin;
import io.sqm.core.Predicate;
import io.sqm.core.UnaryPredicate;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders SQL Server regular joins, including lateral APPLY mappings.
 */
public final class OnJoinRenderer extends io.sqm.render.ansi.OnJoinRenderer {
    /**
     * Creates a SQL Server ON-join renderer.
     */
    public OnJoinRenderer() {
    }

    @Override
    public void render(OnJoin node, RenderContext ctx, SqlWriter w) {
        if (node.right() instanceof Lateral lateral) {
            if (!ctx.dialect().capabilities().supports(SqlFeature.LATERAL)) {
                throw new UnsupportedDialectFeatureException("APPLY", ctx.dialect().name());
            }
            if (!isApplyCompatible(node)) {
                throw new UnsupportedOperationException("SQL Server lateral joins require APPLY-compatible join shape");
            }
            if (node.kind() == JoinKind.LEFT) {
                w.append("OUTER APPLY").space().append(lateral.inner());
                return;
            }
            if (node.kind() == JoinKind.INNER) {
                w.append("CROSS APPLY").space().append(lateral.inner());
                return;
            }
        }
        super.render(node, ctx, w);
    }

    static boolean isApplyCompatible(OnJoin node) {
        return (node.kind() == JoinKind.INNER || node.kind() == JoinKind.LEFT) && isTruePredicate(node.on());
    }

    private static boolean isTruePredicate(Predicate predicate) {
        return predicate instanceof UnaryPredicate unary
            && unary.expr() instanceof LiteralExpr literal
            && Boolean.TRUE.equals(literal.value());
    }
}