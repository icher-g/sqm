package io.sqm.render;

import io.sqm.core.Node;
import io.sqm.core.Statement;
import io.sqm.core.StatementSequence;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.delete;
import static io.sqm.dsl.Dsl.id;
import static io.sqm.dsl.Dsl.insert;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.row;
import static io.sqm.dsl.Dsl.select;
import static io.sqm.dsl.Dsl.update;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class StatementRendererTest {

    @Test
    void delegatesQueryStatementsToQueryRenderer() {
        var dialect = new RenderTestDialect()
            .register(new StatementRenderer())
            .register(new QueryRenderer())
            .register(new SelectRenderer());
        var ctx = RenderContext.of(dialect);

        Statement statement = select(lit(1)).build();
        var sql = ctx.render(statement).sql();

        assertEquals("SELECT-STUB", sql);
    }

    @Test
    void delegatesInsertStatementsToInsertRenderer() {
        var dialect = new RenderTestDialect()
            .register(new StatementRenderer())
            .register(new QueryRenderer())
            .register(new InsertRenderer())
            .register(new SelectRenderer());
        var ctx = RenderContext.of(dialect);

        Statement statement = insert("users")
            .values(row(lit(1)))
            .build();

        var sql = ctx.render(statement).sql();
        assertEquals("INSERT-STUB", sql);
    }

    @Test
    void delegatesUpdateStatementsToUpdateRenderer() {
        var dialect = new RenderTestDialect()
            .register(new StatementRenderer())
            .register(new QueryRenderer())
            .register(new UpdateRenderer())
            .register(new SelectRenderer());
        var ctx = RenderContext.of(dialect);

        Statement statement = update("users")
            .set(id("name"), lit("alice"))
            .build();

        var sql = ctx.render(statement).sql();
        assertEquals("UPDATE-STUB", sql);
    }

    @Test
    void delegatesDeleteStatementsToDeleteRenderer() {
        var dialect = new RenderTestDialect()
            .register(new StatementRenderer())
            .register(new QueryRenderer())
            .register(new DeleteRenderer())
            .register(new SelectRenderer());
        var ctx = RenderContext.of(dialect);

        Statement statement = delete("users").build();

        var sql = ctx.render(statement).sql();
        assertEquals("DELETE-STUB", sql);
    }

    @Test
    void delegatesQueryBranchWhenInvokedDirectly() {
        var writer = new RecordingWriter();
        var ctx = RenderContext.of(new RenderTestDialect());

        new StatementRenderer().render(select(lit(1)).build(), ctx, writer);

        assertInstanceOf(io.sqm.core.Query.class, writer.lastNode);
    }

    @Test
    void delegatesInsertBranchWhenInvokedDirectly() {
        var writer = new RecordingWriter();
        var ctx = RenderContext.of(new RenderTestDialect());

        new StatementRenderer().render(insert("users").values(row(lit(1))).build(), ctx, writer);

        assertInstanceOf(io.sqm.core.InsertStatement.class, writer.lastNode);
    }

    @Test
    void delegatesUpdateBranchWhenInvokedDirectly() {
        var writer = new RecordingWriter();
        var ctx = RenderContext.of(new RenderTestDialect());

        new StatementRenderer().render(update("users").set(id("name"), lit("alice")).build(), ctx, writer);

        assertInstanceOf(io.sqm.core.UpdateStatement.class, writer.lastNode);
    }

    @Test
    void delegatesDeleteBranchWhenInvokedDirectly() {
        var writer = new RecordingWriter();
        var ctx = RenderContext.of(new RenderTestDialect());

        new StatementRenderer().render(delete("users").build(), ctx, writer);

        assertInstanceOf(io.sqm.core.DeleteStatement.class, writer.lastNode);
    }

    @Test
    void rendersStatementSequenceWithSemicolonTerminatedStatements() {
        var dialect = new RenderTestDialect()
            .register(new StatementSequenceRenderer())
            .register(new StatementRenderer())
            .register(new QueryRenderer())
            .register(new SelectRenderer())
            .register(new InsertRenderer());
        var ctx = RenderContext.of(dialect);
        var sequence = StatementSequence.of(
            select(lit(1)).build(),
            insert("users").values(row(lit(2))).build()
        );

        var sql = ctx.render(sequence).sql();

        assertEquals("SELECT-STUB;\nINSERT-STUB;", sql);
    }

    @Test
    void exposesStatementTargetType() {
        assertEquals(Statement.class, new StatementRenderer().targetType());
    }

    @Test
    void exposesStatementSequenceTargetType() {
        assertEquals(StatementSequence.class, new StatementSequenceRenderer().targetType());
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

    private static final class SelectRenderer implements Renderer<io.sqm.core.SelectQuery> {
        @Override
        public void render(io.sqm.core.SelectQuery node, RenderContext ctx, SqlWriter w) {
            w.append("SELECT-STUB");
        }

        @Override
        public Class<io.sqm.core.SelectQuery> targetType() {
            return io.sqm.core.SelectQuery.class;
        }
    }

    private static final class InsertRenderer implements Renderer<io.sqm.core.InsertStatement> {
        @Override
        public void render(io.sqm.core.InsertStatement node, RenderContext ctx, SqlWriter w) {
            w.append("INSERT-STUB");
        }

        @Override
        public Class<io.sqm.core.InsertStatement> targetType() {
            return io.sqm.core.InsertStatement.class;
        }
    }

    private static final class UpdateRenderer implements Renderer<io.sqm.core.UpdateStatement> {
        @Override
        public void render(io.sqm.core.UpdateStatement node, RenderContext ctx, SqlWriter w) {
            w.append("UPDATE-STUB");
        }

        @Override
        public Class<io.sqm.core.UpdateStatement> targetType() {
            return io.sqm.core.UpdateStatement.class;
        }
    }

    private static final class DeleteRenderer implements Renderer<io.sqm.core.DeleteStatement> {
        @Override
        public void render(io.sqm.core.DeleteStatement node, RenderContext ctx, SqlWriter w) {
            w.append("DELETE-STUB");
        }

        @Override
        public Class<io.sqm.core.DeleteStatement> targetType() {
            return io.sqm.core.DeleteStatement.class;
        }
    }
}
