package io.sqm.render.defaults;

import io.sqm.core.ColumnExpr;
import io.sqm.core.Identifier;
import io.sqm.core.LiteralExpr;
import io.sqm.core.Node;
import io.sqm.core.OrdinalParamExpr;
import io.sqm.core.QuoteStyle;
import io.sqm.dsl.Dsl;
import io.sqm.render.RenderTestDialect;
import io.sqm.render.SqlText;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.id;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultSqlWriterTest {

    @Test
    void appendsStringsSpacesAndIgnoresEmpty() {
        var dialect = new RenderTestDialect();
        var ctx = RenderContext.of(dialect);
        var writer = new DefaultSqlWriter(ctx);

        writer.append("SELECT").space().append("1").append("").append((String) null);

        assertEquals("SELECT 1", writer.toText(List.of()).sql());
    }

    @Test
    void appendsNodesAndCommaSeparatedLists() {
        var dialect = new RenderTestDialect()
            .register(new ColumnRenderer());
        var ctx = RenderContext.of(dialect);
        var writer = new DefaultSqlWriter(ctx);

        writer.append(Dsl.col("a"));
        writer.space();
        writer.comma(List.of(Dsl.col("b"), Dsl.col("c")), true);

        assertEquals("a (b), (c)", writer.toText(List.of()).sql());
    }

    @Test
    void appendsNodeUsingExplicitTypeRenderer() {
        var dialect = new RenderTestDialect()
            .register(new RowValuesRootRenderer())
            .register(new RowExprRenderer());
        var ctx = RenderContext.of(dialect);
        var writer = new DefaultSqlWriter(ctx);

        var value = Dsl.row(1);
        writer.append(io.sqm.core.RowValues.class, value);

        assertEquals("ROW-VALUES-STUB", writer.toText(List.of()).sql());
    }

    @Test
    void appendsCommaSeparatedIdentifiers() {
        var dialect = new RenderTestDialect();
        var ctx = RenderContext.of(dialect);
        var writer = new DefaultSqlWriter(ctx);

        writer.comma(List.of(id("id"), id("name")), ctx.dialect().quoter());

        assertEquals("\"id\", \"name\"", writer.toText(List.of()).sql());
    }

    @Test
    void appendsQuotedIdentifiersWithStyleFallback() {
        var dialect = new RenderTestDialect();
        var ctx = RenderContext.of(dialect);
        var writer = new DefaultSqlWriter(ctx);

        writer.comma(List.of(
            Identifier.of("standard", QuoteStyle.DOUBLE_QUOTE),
            Identifier.of("legacy", QuoteStyle.BACKTICK)),
            ctx.dialect().quoter());

        assertEquals("\"standard\", \"legacy\"", writer.toText(List.of()).sql());
    }

    @Test
    void defaultAppendWithExplicitTypeFallsBackToAppendNode() {
        var writer = new MinimalWriter();
        writer.append(ColumnExpr.class, Dsl.col("x"));

        assertEquals("x", writer.sql());
    }

    @Test
    void supportsSingleLineMode() {
        var dialect = new RenderTestDialect()
            .register(new ColumnRenderer());
        var ctx = RenderContext.of(dialect);
        var writer = new DefaultSqlWriter(ctx);

        writer.append("a");
        writer.singleLine();
        writer.newline();
        writer.append("b");
        writer.multiLine();
        writer.newline();
        writer.append("c");

        assertEquals("a b\nc", writer.toText(List.of()).sql());
    }

    @Test
    void enclosesAndIndentsMultiline() {
        var dialect = new RenderTestDialect()
            .register(new ColumnRenderer());
        var ctx = RenderContext.of(dialect);
        SqlWriter writer = new DefaultSqlWriter(ctx, 2);

        writer.append(Dsl.col("col"), true, true);

        assertEquals("(\n  col\n)", writer.toText(List.of()).sql());
    }

    @Test
    void toTextReturnsParams() {
        var dialect = new RenderTestDialect()
            .register(new LiteralRenderer())
            .register(new ParamRenderer());
        var ctx = RenderContext.of(dialect);
        var writer = new DefaultSqlWriter(ctx);

        writer.append(OrdinalParamExpr.of(1));
        SqlText text = writer.toText(List.of(10));

        assertEquals("$1", text.sql());
        assertEquals(List.of(10), text.params());
    }

    private static final class MinimalWriter implements SqlWriter {
        private final StringBuilder sb = new StringBuilder();

        @Override
        public SqlWriter append(String s) {
            if (s != null) {
                sb.append(s);
            }
            return this;
        }

        @Override
        public <T extends Node> SqlWriter append(T node) {
            if (node instanceof ColumnExpr c) {
                sb.append(c.name().value());
            }
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
            sb.append(' ');
            return this;
        }

        @Override
        public SqlWriter newline() {
            sb.append('\n');
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
            return new io.sqm.render.RenderResult(sb.toString(), params);
        }

        String sql() {
            return sb.toString();
        }
    }

    private static final class ColumnRenderer implements Renderer<ColumnExpr> {
        @Override
        public void render(ColumnExpr node, RenderContext ctx, SqlWriter w) {
            w.append(node.name().value());
        }

        @Override
        public Class<ColumnExpr> targetType() {
            return ColumnExpr.class;
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

    private static final class RowValuesRootRenderer implements Renderer<io.sqm.core.RowValues> {
        @Override
        public void render(io.sqm.core.RowValues node, RenderContext ctx, SqlWriter w) {
            w.append("ROW-VALUES-STUB");
        }

        @Override
        public Class<io.sqm.core.RowValues> targetType() {
            return io.sqm.core.RowValues.class;
        }
    }

    private static final class LiteralRenderer implements Renderer<LiteralExpr> {
        @Override
        public void render(LiteralExpr node, RenderContext ctx, SqlWriter w) {
            w.append(ctx.dialect().formatter().format(node.value()));
        }

        @Override
        public Class<LiteralExpr> targetType() {
            return LiteralExpr.class;
        }
    }

    private static final class ParamRenderer implements Renderer<OrdinalParamExpr> {
        @Override
        public void render(OrdinalParamExpr node, RenderContext ctx, SqlWriter w) {
            w.append("$" + node.index());
        }

        @Override
        public Class<OrdinalParamExpr> targetType() {
            return OrdinalParamExpr.class;
        }
    }
}
