package io.sqm.render.ansi;

import io.sqm.core.MatchPattern;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

final class PatternRenderSupport {
    private PatternRenderSupport() {
    }

    static void requireSupport(RenderContext ctx) {
        if (!ctx.dialect().capabilities().supports(SqlFeature.MATCH_RECOGNIZE)) {
            throw new UnsupportedDialectFeatureException("MATCH_RECOGNIZE", ctx.dialect().name());
        }
    }

    static void renderChild(MatchPattern pattern, int parentPrecedence, RenderContext ctx, SqlWriter w) {
        boolean enclosed = precedence(pattern) < parentPrecedence
            || pattern instanceof MatchPattern.Quantified && parentPrecedence == 3;
        if (enclosed) w.append("(");
        w.append(MatchPattern.class, pattern);
        if (enclosed) w.append(")");
    }

    static int precedence(MatchPattern pattern) {
        if (pattern instanceof MatchPattern.Alternation) return 1;
        if (pattern instanceof MatchPattern.Sequence) return 2;
        if (pattern instanceof MatchPattern.Quantified) return 3;
        return 4;
    }
}
