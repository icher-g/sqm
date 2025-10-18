package io.sqm.render.ansi.query;

import io.sqm.core.CompositeQuery;
import io.sqm.core.SelectQuery;
import io.sqm.render.ansi.LimitOffsetRenderer;
import io.sqm.render.spi.Renderer;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

public final class CompositeQueryRenderer implements Renderer<CompositeQuery> {

    @Override
    public Class<CompositeQuery> targetType() {
        return CompositeQuery.class;
    }

    @Override
    public void render(CompositeQuery entity, RenderContext ctx, SqlWriter w) {
        var terms = entity.terms();
        var ops = entity.ops();

        // Validate ANSI constraints on each term (no per-term ORDER/LIMIT/OFFSET)
        for (int i = 0; i < terms.size(); i++) {
            if (terms.get(i) instanceof SelectQuery t) {
                if (t.limit() != null || t.offset() != null || (t.orderBy() != null && !t.orderBy().items().isEmpty())) {
                    throw new UnsupportedOperationException(
                        "ANSI: operands of UNION/INTERSECT/EXCEPT must not have their own ORDER BY / LIMIT / OFFSET (term #" + (i + 1) + ")"
                    );
                }
            }
        }

        // Render terms with safe parentheses
        for (int i = 0; i < terms.size(); i++) {
            var term = terms.get(i);

            boolean needsParens = terms.size() > 1;
            if (needsParens) {
                w.append("(").newline();
                w.indent();
            }

            w.append(term);

            if (needsParens) {
                w.outdent();
                w.newline().append(")");
            }

            if (i < ops.size()) {
                w.newline();
                renderOp(ops.get(i), w);
                w.newline();
            }
        }

        // Final ORDER BY (applies to the whole composite)
        if (entity.orderBy() != null) {
            w.newline().append("ORDER BY").space();
            w.append(entity.orderBy());
        }

        // Final pagination (ANSI OFFSET/FETCH)
        // Pagination tail — pick the right style
        new LimitOffsetRenderer().render(entity, ctx, w);
    }

    private void renderOp(CompositeQuery.Op op, SqlWriter w) {
        switch (op.kind()) {
            case Union -> w.append("UNION");
            case Intersect -> w.append("INTERSECT");
            case Except -> w.append("EXCEPT");
            default -> throw new UnsupportedOperationException("Unknown set op: " + op.kind());
        }
        if (op.all()) w.space().append("ALL");
    }
}
