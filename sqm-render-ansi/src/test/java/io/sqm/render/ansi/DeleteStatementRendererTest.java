package io.sqm.render.ansi;

import io.sqm.core.DeleteStatement;
import io.sqm.core.Statement;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.delete;
import static io.sqm.dsl.Dsl.inner;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.tbl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class DeleteStatementRendererTest {

    @Test
    void rendersDeleteWithWhere() {
        var ctx = RenderContext.of(new io.sqm.render.ansi.spi.AnsiDialect());
        DeleteStatement statement = delete("users")
            .where(col("id").eq(lit(1)))
            .build();

        var sql = normalize(ctx.render(statement).sql());

        assertEquals("DELETE FROM users WHERE id = 1", sql);
    }

    @Test
    void rendersDeleteWithoutWhere() {
        var ctx = RenderContext.of(new io.sqm.render.ansi.spi.AnsiDialect());
        DeleteStatement statement = delete("users").build();

        var sql = normalize(ctx.render(statement).sql());

        assertEquals("DELETE FROM users", sql);
    }

    @Test
    void statementRootRenderingSupportsDelete() {
        var ctx = RenderContext.of(new io.sqm.render.ansi.spi.AnsiDialect());
        Statement statement = delete("users").build();

        var sql = normalize(ctx.render(statement).sql());

        assertEquals("DELETE FROM users", sql);
    }

    @Test
    void ansiRegistryProvidesDeleteRenderer() {
        var repo = Renderers.ansi();
        assertInstanceOf(DeleteStatementRenderer.class, repo.require(DeleteStatement.class));
    }

    @Test
    void rejectsDeleteUsingWhenDialectDoesNotSupportIt() {
        var ctx = RenderContext.of(new io.sqm.render.ansi.spi.AnsiDialect());
        DeleteStatement statement = delete("users")
            .using(io.sqm.dsl.Dsl.tbl("source_users"))
            .build();

        var renderer = new DeleteStatementRenderer();
        var writer = new io.sqm.render.defaults.DefaultSqlWriter(ctx);

        org.junit.jupiter.api.Assertions.assertThrows(
            io.sqm.core.dialect.UnsupportedDialectFeatureException.class,
            () -> renderer.render(statement, ctx, writer)
        );
    }

    @Test
    void rejectsDeleteJoinWhenDialectDoesNotSupportIt() {
        var ctx = RenderContext.of(new io.sqm.render.ansi.spi.AnsiDialect());
        DeleteStatement statement = delete(tbl("users"))
            .using(tbl("users"))
            .join(inner(tbl("orders")).on(col("users", "id").eq(col("orders", "user_id"))))
            .build();

        var renderer = new DeleteStatementRenderer();
        var writer = new io.sqm.render.defaults.DefaultSqlWriter(ctx);

        org.junit.jupiter.api.Assertions.assertThrows(
            io.sqm.core.dialect.UnsupportedDialectFeatureException.class,
            () -> renderer.render(statement, ctx, writer)
        );
    }

    @Test
    void rejectsDeleteReturningWhenDialectDoesNotSupportIt() {
        var ctx = RenderContext.of(new io.sqm.render.ansi.spi.AnsiDialect());
        DeleteStatement statement = DeleteStatement.of(
            tbl("users"),
            java.util.List.of(),
            java.util.List.of(),
            null,
            null,
            java.util.List.of(io.sqm.core.ExprSelectItem.of(col("id"), null)),
            java.util.List.of());

        var renderer = new DeleteStatementRenderer();
        var writer = new io.sqm.render.defaults.DefaultSqlWriter(ctx);

        org.junit.jupiter.api.Assertions.assertThrows(
            io.sqm.core.dialect.UnsupportedDialectFeatureException.class,
            () -> renderer.render(statement, ctx, writer)
        );
    }

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
