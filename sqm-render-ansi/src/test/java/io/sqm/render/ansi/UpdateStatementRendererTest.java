package io.sqm.render.ansi;

import io.sqm.core.Statement;
import io.sqm.core.UpdateStatement;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.id;
import static io.sqm.dsl.Dsl.inner;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.tbl;
import static io.sqm.dsl.Dsl.update;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class UpdateStatementRendererTest {

    @Test
    void rendersUpdateWithWhere() {
        var ctx = RenderContext.of(new io.sqm.render.ansi.spi.AnsiDialect());
        UpdateStatement statement = update("users")
            .set(id("name"), lit("alice"))
            .set(id("active"), lit(true))
            .where(io.sqm.dsl.Dsl.col("id").eq(lit(1)))
            .build();

        var sql = normalize(ctx.render(statement).sql());

        assertEquals("UPDATE users SET name = 'alice', active = TRUE WHERE id = 1", sql);
    }

    @Test
    void rendersUpdateWithQualifiedAssignmentTarget() {
        var ctx = RenderContext.of(new io.sqm.render.ansi.spi.AnsiDialect());
        UpdateStatement statement = update(tbl("users").as("u"))
            .set(io.sqm.core.QualifiedName.of("u", "name"), lit("alice"))
            .build();

        var sql = normalize(ctx.render(statement).sql());

        assertEquals("UPDATE users AS u SET u.name = 'alice'", sql);
    }

    @Test
    void rendersUpdateWithoutWhere() {
        var ctx = RenderContext.of(new io.sqm.render.ansi.spi.AnsiDialect());
        UpdateStatement statement = update("users")
            .set(id("name"), lit("alice"))
            .build();

        var sql = normalize(ctx.render(statement).sql());

        assertEquals("UPDATE users SET name = 'alice'", sql);
    }

    @Test
    void rendersUpdateClausesOnSeparateLines() {
        var ctx = RenderContext.of(new io.sqm.render.ansi.spi.AnsiDialect());
        UpdateStatement statement = update("users")
            .set(id("name"), lit("alice"))
            .where(io.sqm.dsl.Dsl.col("id").eq(lit(1)))
            .build();

        var sql = ctx.render(statement).sql();

        assertEquals(
            """
                UPDATE users
                SET name = 'alice'
                WHERE id = 1
                """.trim(),
            sql
        );
    }

    @Test
    void statementRootRenderingSupportsUpdate() {
        var ctx = RenderContext.of(new io.sqm.render.ansi.spi.AnsiDialect());
        Statement statement = update("users")
            .set(id("name"), lit("alice"))
            .build();

        var sql = normalize(ctx.render(statement).sql());

        assertEquals("UPDATE users SET name = 'alice'", sql);
    }

    @Test
    void ansiRegistryProvidesUpdateRenderer() {
        var repo = Renderers.ansi();
        assertInstanceOf(UpdateStatementRenderer.class, repo.require(UpdateStatement.class));
    }

    @Test
    void rejectsUpdateJoinWhenDialectDoesNotSupportIt() {
        var ctx = RenderContext.of(new io.sqm.render.ansi.spi.AnsiDialect());
        UpdateStatement statement = update(tbl("users"))
            .join(inner(tbl("orders")).on(col("users", "id").eq(col("orders", "user_id"))))
            .set(id("name"), lit("alice"))
            .build();

        var renderer = new UpdateStatementRenderer();
        var writer = new io.sqm.render.defaults.DefaultSqlWriter(ctx);

        org.junit.jupiter.api.Assertions.assertThrows(
            io.sqm.core.dialect.UnsupportedDialectFeatureException.class,
            () -> renderer.render(statement, ctx, writer)
        );
    }

    @Test
    void rejectsUpdateFromWhenDialectDoesNotSupportIt() {
        var ctx = RenderContext.of(new io.sqm.render.ansi.spi.AnsiDialect());
        UpdateStatement statement = update("users")
            .set(id("name"), lit("alice"))
            .from(io.sqm.dsl.Dsl.tbl("source_users"))
            .build();

        var renderer = new UpdateStatementRenderer();
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
