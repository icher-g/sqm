package io.cherlabs.sqlmodel.render;

import io.cherlabs.sqlmodel.render.spi.RenderContext;

import java.util.List;
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
        var w = new DefaultSqlWriter(ctx);
        render.accept(w);
        return w.toText(List.of()).sql();
    }
}
