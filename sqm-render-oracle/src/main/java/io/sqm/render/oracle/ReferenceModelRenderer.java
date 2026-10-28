package io.sqm.render.oracle;

import io.sqm.core.NavigationMode;
import io.sqm.core.ReferenceModel;
import io.sqm.core.UniquenessMode;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders Oracle MODEL syntax such as {@code REFERENCE rates ON (SELECT code, rate FROM fx) DIMENSION BY (code) MEASURES (rate)} through registered child renderers.
 */
public class ReferenceModelRenderer extends io.sqm.render.ansi.ReferenceModelRenderer {
    /**
     * Creates an Oracle ReferenceModel renderer.
     */
    public ReferenceModelRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(ReferenceModel node, RenderContext ctx, SqlWriter w) {
        ModelRenderSupport.require(ctx);
        w.append("REFERENCE ").append(renderIdentifier(node.name(), ctx.dialect().quoter()))
            .append(" ON ").append(node.query(), true, true).newline().indent();
        w.append("DIMENSION BY (").comma(node.dimensions()).append(")").newline();
        w.append("MEASURES (").comma(node.measures()).append(")");
        if (node.navigationMode() == NavigationMode.IGNORE) w.append(" IGNORE NAV");
        if (node.uniquenessMode() == UniquenessMode.SINGLE_REFERENCE) w.append(" UNIQUE SINGLE REFERENCE");
        w.outdent();
    }
}
