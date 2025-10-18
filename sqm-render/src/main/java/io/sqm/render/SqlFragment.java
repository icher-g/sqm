package io.sqm.render;

import io.sqm.render.spi.RenderContext;

import java.util.function.Consumer;

/**
 * A helper class to render a portion of a query and get its string representation.
 */
public class SqlFragment {
    private SqlFragment() {
    }

    /**
     * Render the given lambda into a String using an internal buffer writer.
     */
    public static String capture(RenderContext ctx, Consumer<SqlWriter> render) {
        var c = RenderContext.of(ctx.dialect()); // create new RenderContext to have new ParamSink instance.
        var w = new DefaultSqlWriter(c);
        render.accept(w);
        return w.toText(c.params().snapshot()).sql();
    }
}
