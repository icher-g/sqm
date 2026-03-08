package io.sqm.render;

import io.sqm.core.CompositeQuery;
import io.sqm.core.DialectQuery;
import io.sqm.core.Node;
import io.sqm.core.Query;
import io.sqm.core.SelectQuery;
import io.sqm.core.WithQuery;
import io.sqm.core.walk.NodeVisitor;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.cte;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.select;
import static io.sqm.dsl.Dsl.with;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QueryRendererTest {

    @Test
    void delegatesToSelectQueryRenderer() {
        var dialect = new RenderTestDialect()
            .register(new QueryRenderer())
            .register(new SelectRenderer());
        var ctx = RenderContext.of(dialect);

        Query query = select(lit(1)).build();
        var sql = ctx.render(query).sql();

        assertEquals("SELECT-STUB", sql);
    }

    @Test
    void delegatesToWithQueryRenderer() {
        var dialect = new RenderTestDialect()
            .register(new QueryRenderer())
            .register(new WithRenderer());
        var ctx = RenderContext.of(dialect);

        WithQuery query = with(cte("c", select(lit(1)).build())).body(select(lit(2)).build());
        var sql = ctx.render(query).sql();

        assertEquals("WITH-STUB", sql);
    }

    @Test
    void delegatesToCompositeQueryRenderer() {
        var dialect = new RenderTestDialect()
            .register(new QueryRenderer())
            .register(new CompositeRenderer());
        var ctx = RenderContext.of(dialect);

        CompositeQuery query = select(lit(1)).build().union(select(lit(2)).build());
        var sql = ctx.render(query).sql();

        assertEquals("COMPOSITE-STUB", sql);
    }

    @Test
    void delegatesSelectBranchWhenInvokedDirectly() {
        var writer = new RecordingWriter();
        var ctx = RenderContext.of(new RenderTestDialect());

        new QueryRenderer().render(select(lit(1)).build(), ctx, writer);

        assertInstanceOf(SelectQuery.class, writer.lastNode);
    }

    @Test
    void delegatesWithBranchWhenInvokedDirectly() {
        var writer = new RecordingWriter();
        var ctx = RenderContext.of(new RenderTestDialect());

        var node = with(cte("c", select(lit(1)).build())).body(select(lit(2)).build());
        new QueryRenderer().render(node, ctx, writer);

        assertInstanceOf(WithQuery.class, writer.lastNode);
    }

    @Test
    void delegatesCompositeBranchWhenInvokedDirectly() {
        var writer = new RecordingWriter();
        var ctx = RenderContext.of(new RenderTestDialect());

        var node = select(lit(1)).build().union(select(lit(2)).build());
        new QueryRenderer().render(node, ctx, writer);

        assertInstanceOf(CompositeQuery.class, writer.lastNode);
    }

    @Test
    void rejectsUnsupportedDialectQueryType() {
        var dialect = new RenderTestDialect();
        var ctx = RenderContext.of(dialect);
        var writer = new io.sqm.render.defaults.DefaultSqlWriter(ctx);

        Query unsupported = new DialectQuery() {
            @Override
            public <R> R accept(NodeVisitor<R> v) {
                return null;
            }
        };

        var ex = assertThrows(IllegalArgumentException.class, () -> new QueryRenderer().render(unsupported, ctx, writer));
        assertEquals("Unsupported query type: " + unsupported.getClass().getName(), ex.getMessage());
    }

    @Test
    void exposesQueryTargetType() {
        assertEquals(Query.class, new QueryRenderer().targetType());
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

    private static final class SelectRenderer implements Renderer<SelectQuery> {
        @Override
        public void render(SelectQuery node, RenderContext ctx, SqlWriter w) {
            w.append("SELECT-STUB");
        }

        @Override
        public Class<SelectQuery> targetType() {
            return SelectQuery.class;
        }
    }

    private static final class WithRenderer implements Renderer<WithQuery> {
        @Override
        public void render(WithQuery node, RenderContext ctx, SqlWriter w) {
            w.append("WITH-STUB");
        }

        @Override
        public Class<WithQuery> targetType() {
            return WithQuery.class;
        }
    }

    private static final class CompositeRenderer implements Renderer<CompositeQuery> {
        @Override
        public void render(CompositeQuery node, RenderContext ctx, SqlWriter w) {
            w.append("COMPOSITE-STUB");
        }

        @Override
        public Class<CompositeQuery> targetType() {
            return CompositeQuery.class;
        }
    }
}
