package io.sqm.render.ansi;

import io.sqm.core.MatchPattern;
import io.sqm.core.PatternRecognitionTable;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders a relational {@code MATCH_RECOGNIZE} transform.
 */
public class PatternRecognitionTableRenderer implements Renderer<PatternRecognitionTable> {
    /**
     * Creates a pattern-recognition table renderer.
     */
    public PatternRecognitionTableRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(PatternRecognitionTable node, RenderContext ctx, SqlWriter w) {
        PatternRenderSupport.requireSupport(ctx);
        w.append(node.source()).newline().append("MATCH_RECOGNIZE").space().append("(").newline().indent();
        if (node.partitionBy() != null) w.append(node.partitionBy()).newline();
        if (node.orderBy() != null) w.append(node.orderBy()).newline();
        if (!node.measures().isEmpty()) {
            w.append("MEASURES").space().comma(node.measures()).newline();
        }
        w.append(node.rowsPerMatch()).newline();
        w.append(node.afterMatchSkip()).newline();
        w.append("PATTERN").space().append("(").append(MatchPattern.class, node.pattern()).append(")").newline();
        if (!node.subsets().isEmpty()) {
            w.append("SUBSET").space().comma(node.subsets()).newline();
        }
        w.append("DEFINE").space().comma(node.definitions()).outdent().newline().append(")");
        renderTableAlias(node.alias(), null, ctx, w);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<PatternRecognitionTable> targetType() {
        return PatternRecognitionTable.class;
    }
}
