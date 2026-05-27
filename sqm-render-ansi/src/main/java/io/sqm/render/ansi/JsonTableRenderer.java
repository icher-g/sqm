package io.sqm.render.ansi;

import io.sqm.core.*;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders SQL/JSON {@code JSON_TABLE} table references.
 */
public class JsonTableRenderer implements Renderer<JsonTable> {
    /**
     * Creates a JSON table-reference renderer.
     */
    public JsonTableRenderer() {
    }

    @Override
    public void render(JsonTable node, RenderContext ctx, SqlWriter w) {
        if (!ctx.dialect().capabilities().supports(SqlFeature.JSON_TABLE)) {
            throw new UnsupportedDialectFeatureException("JSON_TABLE", ctx.dialect().name());
        }
        w.append("JSON_TABLE")
            .space()
            .append("(").newline().indent()
            .append(node.json())
            .append(",")
            .space()
            .append(quotePath(node.rootPath().text()))
            .newline()
            .append("COLUMNS")
            .space()
            .append("(").newline().indent();
        renderColumns(node.columns(), ctx, w);
        w.outdent().newline().append(")");
        w.outdent().newline().append(")");
        if (node.alias() != null) {
            w.space().append("AS").space().append(renderIdentifier(node.alias(), ctx.dialect().quoter()));
        }
    }

    @Override
    public Class<? extends JsonTable> targetType() {
        return JsonTable.class;
    }

    private void renderColumns(Iterable<JsonTableColumn> columns, RenderContext ctx, SqlWriter w) {
        boolean first = true;
        for (var column : columns) {
            if (!first) {
                w.append(",").newline();
            }
            renderColumn(column, ctx, w);
            first = false;
        }
    }

    private void renderColumn(JsonTableColumn column, RenderContext ctx, SqlWriter w) {
        if (column instanceof JsonTableScalarColumn scalarColumn) {
            w.append(renderIdentifier(scalarColumn.name(), ctx.dialect().quoter()))
                .space()
                .append(scalarColumn.type())
                .space()
                .append("PATH")
                .space()
                .append(quotePath(scalarColumn.path().text()));
            renderWrapper(scalarColumn.wrapper(), w);
            renderBehavior("EMPTY", scalarColumn.onEmpty(), w);
            renderBehavior("ERROR", scalarColumn.onError(), w);
            return;
        }
        if (column instanceof JsonTableOrdinalityColumn ordinalityColumn) {
            w.append(renderIdentifier(ordinalityColumn.name(), ctx.dialect().quoter()))
                .space()
                .append("FOR ORDINALITY");
            return;
        }
        if (column instanceof JsonTableExistsColumn existsColumn) {
            w.append(renderIdentifier(existsColumn.name(), ctx.dialect().quoter()))
                .space()
                .append(existsColumn.type())
                .space()
                .append("EXISTS PATH")
                .space()
                .append(quotePath(existsColumn.path().text()));
            renderBehavior("ERROR", existsColumn.onError(), w);
            return;
        }
        if (column instanceof JsonTableNestedPathColumn pathColumn) {
            w.append("NESTED PATH")
                .space()
                .append(quotePath(pathColumn.path().text()))
                .space()
                .append("COLUMNS")
                .space()
                .append("(").newline().indent();
            renderColumns(pathColumn.columns(), ctx, w);
            w.outdent().newline().append(")");
            return;
        }
        throw new IllegalArgumentException("Unsupported JSON_TABLE column: " + column.getClass().getName());
    }

    private void renderWrapper(JsonTableScalarColumn.Wrapper wrapper, SqlWriter w) {
        switch (wrapper) {
            case DEFAULT -> {
            }
            case WITHOUT -> w.space().append("WITHOUT WRAPPER");
            case WITH -> w.space().append("WITH WRAPPER");
            case CONDITIONAL -> w.space().append("WITH CONDITIONAL WRAPPER");
        }
    }

    private void renderBehavior(String condition, JsonTableBehavior behavior, SqlWriter w) {
        if (behavior == null) {
            return;
        }
        w.space();
        switch (behavior.kind()) {
            case ERROR -> w.append("ERROR");
            case NULL -> w.append("NULL");
            case EMPTY -> w.append("EMPTY");
            case DEFAULT -> w.append("DEFAULT").space().append(behavior.defaultExpression());
        }
        w.space().append("ON").space().append(condition);
    }

    private String quotePath(String text) {
        return "'" + text.replace("'", "''") + "'";
    }
}
