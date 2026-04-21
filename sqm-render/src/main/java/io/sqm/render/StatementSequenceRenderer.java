package io.sqm.render;

import io.sqm.core.StatementSequence;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders ordered statement sequences as SQL batches.
 */
public class StatementSequenceRenderer implements Renderer<StatementSequence> {

    /**
     * Creates a statement-sequence renderer.
     */
    public StatementSequenceRenderer() {
    }

    /**
     * Renders each statement in source order and terminates every rendered statement with a semicolon.
     *
     * @param node statement sequence to render
     * @param ctx rendering context
     * @param w SQL writer
     */
    @Override
    public void render(StatementSequence node, RenderContext ctx, SqlWriter w) {
        var statements = node.statements();
        for (int i = 0; i < statements.size(); i++) {
            if (i > 0) {
                w.append(";");
                w.newline();
            }
            w.append(statements.get(i));
        }
        if (statements.size() > 1) {
            w.append(";");
        }
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return statement-sequence root type
     */
    @Override
    public Class<StatementSequence> targetType() {
        return StatementSequence.class;
    }
}
