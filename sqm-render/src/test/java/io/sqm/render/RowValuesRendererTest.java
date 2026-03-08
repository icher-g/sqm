package io.sqm.render;

import io.sqm.core.Node;
import io.sqm.core.RowValues;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.row;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class RowValuesRendererTest {

    @Test
    void delegatesSingleRowToRowExprRenderer() {
        var dialect = new RenderTestDialect()
            .register(new RowValuesRenderer())
            .register(new RowExprRenderer());
        var ctx = RenderContext.of(dialect);

        RowValues rowValues = row(lit(1));
        var sql = ctx.render(rowValues).sql();

        assertEquals("ROW-EXPR-STUB", sql);
    }

    @Test
    void delegatesRowListToRowListRenderer() {
        var dialect = new RenderTestDialect()
            .register(new RowValuesRenderer())
            .register(new RowListRenderer());
        var ctx = RenderContext.of(dialect);

        RowValues rowValues = io.sqm.core.RowListExpr.of(java.util.List.of(row(lit(1)), row(lit(2))));
        var sql = ctx.render(rowValues).sql();

        assertEquals("ROW-LIST-STUB", sql);
    }

    @Test
    void delegatesRowExprBranchWhenInvokedDirectly() {
        var writer = new RecordingWriter();
        var ctx = RenderContext.of(new RenderTestDialect());

        new RowValuesRenderer().render(row(lit(1)), ctx, writer);

        assertInstanceOf(io.sqm.core.RowExpr.class, writer.lastNode);
    }

    @Test
    void delegatesRowListBranchWhenInvokedDirectly() {
        var writer = new RecordingWriter();
        var ctx = RenderContext.of(new RenderTestDialect());

        var rowList = io.sqm.core.RowListExpr.of(java.util.List.of(row(lit(1)), row(lit(2))));
        new RowValuesRenderer().render(rowList, ctx, writer);

        assertInstanceOf(io.sqm.core.RowListExpr.class, writer.lastNode);
    }

    @Test
    void exposesRowValuesTargetType() {
        assertEquals(RowValues.class, new RowValuesRenderer().targetType());
    }

    private static final class RecordingWriter implements SqlWriter {
        private Node lastNode;

        @Override
        public SqlWriter append(String s) {
            return this;
        }

        @Override
        public <T extends Node> SqlWriter append(T node) {
            this.lastNode = node;
            return this;
        }

        @Override
        public void singleLine() {
        }

        @Override
        public void multiLine() {
        }

        @Override
        public SqlWriter space() {
            return this;
        }

        @Override
        public SqlWriter newline() {
            return this;
        }

        @Override
        public SqlWriter indent() {
            return this;
        }

        @Override
        public SqlWriter outdent() {
            return this;
        }

        @Override
        public SqlText toText(List<Object> params) {
            return new RenderResult("", params);
        }
    }

    private static final class RowExprRenderer implements Renderer<io.sqm.core.RowExpr> {
        @Override
        public void render(io.sqm.core.RowExpr node, RenderContext ctx, SqlWriter w) {
            w.append("ROW-EXPR-STUB");
        }

        @Override
        public Class<io.sqm.core.RowExpr> targetType() {
            return io.sqm.core.RowExpr.class;
        }
    }

    private static final class RowListRenderer implements Renderer<io.sqm.core.RowListExpr> {
        @Override
        public void render(io.sqm.core.RowListExpr node, RenderContext ctx, SqlWriter w) {
            w.append("ROW-LIST-STUB");
        }

        @Override
        public Class<io.sqm.core.RowListExpr> targetType() {
            return io.sqm.core.RowListExpr.class;
        }
    }
}
