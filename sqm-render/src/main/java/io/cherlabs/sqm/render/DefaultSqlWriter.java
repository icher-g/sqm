package io.cherlabs.sqm.render;

import io.cherlabs.sqm.core.Entity;
import io.cherlabs.sqm.render.spi.RenderContext;

import java.util.List;

/**
 * A default implementation of the {@link SqlWriter}. This implementation uses {@link StringBuilder} to keep the written SQL in memory.
 */
public class DefaultSqlWriter implements SqlWriter {

    private final StringBuilder sb = new StringBuilder();
    private final RenderContext ctx;
    private final int indentSize;
    private boolean ignoreNewLine = false;
    private int indentLevel = 0;
    private boolean atLineStart = true;

    public DefaultSqlWriter(RenderContext ctx) {
        this(ctx, 2);
    }

    public DefaultSqlWriter(RenderContext ctx, int indentSize) {
        this.ctx = ctx;
        this.indentSize = Math.max(0, indentSize);
    }

    @Override
    public SqlWriter append(String s) {
        if (s == null || s.isEmpty()) {
            return this;
        }
        writeWithIndentIfNeeded(s);
        return this;
    }

    @Override
    public <T extends Entity> SqlWriter append(T entity) {
        var r = ctx.dialect().renderers().requireFor(entity);
        r.render(entity, ctx, this);
        return this;
    }

    @Override
    public void ignoreNewLine(boolean ignore) {
        ignoreNewLine = ignore;
    }

    @Override
    public SqlWriter space() {
        // no leading spaces at the start of a line
        if (atLineStart) return this;

        int len = sb.length();
        if (len == 0) return this;

        char last = sb.charAt(len - 1);
        if (!Character.isWhitespace(last)) {
            sb.append(' ');
        }
        return this;
    }

    @Override
    public SqlWriter newline() {
        if (!ignoreNewLine) {
            sb.append('\n');
            atLineStart = true;
        } else {
            space();
        }
        return this;
    }

    @Override
    public SqlWriter indent() {
        if (!ignoreNewLine) {
            indentLevel++;
        }
        return this;
    }

    @Override
    public SqlWriter outdent() {
        if (indentLevel > 0) indentLevel--;
        return this;
    }

    @Override
    public <T extends Entity> SqlWriter comma(List<T> parts) {
        if (parts == null || parts.isEmpty()) return this;

        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                sb.append(',');
                sb.append(' ');
            }
            append(parts.get(i));
        }
        return this;
    }

    @Override
    public SqlText toText(List<Object> params) {
        return new RenderResult(sb.toString(), params);
    }

    // --- helpers ---

    private void writeWithIndentIfNeeded(String s) {
        if (atLineStart) {
            if (indentLevel > 0 && indentSize > 0) {
                int spaces = indentLevel * indentSize;
                var indent = " ".repeat(Math.max(0, spaces));
                sb.append(indent);
                s = s.replace("\n", "\n" + indent);
            }
            atLineStart = false;
        }
        sb.append(s);
    }
}
