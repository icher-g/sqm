package io.sqm.render.oracle;

import io.sqm.core.MainModel;
import io.sqm.core.NavigationMode;
import io.sqm.core.UniquenessMode;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders basic Oracle MODEL MainModel syntax through registered child renderers.
 */
public class MainModelRenderer extends io.sqm.render.ansi.MainModelRenderer {
    /**
     * Creates an Oracle MainModel renderer.
     */
    public MainModelRenderer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(MainModel node, RenderContext ctx, SqlWriter w) {
        ModelRenderSupport.require(ctx);
        if (node.name() != null) w.append("MAIN ").append(renderIdentifier(node.name(), ctx.dialect().quoter())).newline().indent();
        if (!node.partitions().isEmpty()) {
            w.append("PARTITION BY (").comma(node.partitions()).append(")").newline();
        }
        w.append("DIMENSION BY (").comma(node.dimensions()).append(")").newline();
        w.append("MEASURES (").comma(node.measures()).append(")");
        if (node.navigationMode() == NavigationMode.IGNORE) w.space().append("IGNORE NAV");
        if (node.uniquenessMode() == UniquenessMode.SINGLE_REFERENCE) w.space().append("UNIQUE SINGLE REFERENCE");
        w.newline().append(node.rules());
        if (node.name() != null) w.outdent();
    }
}
