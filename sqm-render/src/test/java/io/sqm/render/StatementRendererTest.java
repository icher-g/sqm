package io.sqm.render;

import io.sqm.core.Statement;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.delete;
import static io.sqm.dsl.Dsl.id;
import static io.sqm.dsl.Dsl.insert;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.row;
import static io.sqm.dsl.Dsl.select;
import static io.sqm.dsl.Dsl.update;
import static org.junit.jupiter.api.Assertions.assertEquals;

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