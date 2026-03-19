package io.sqm.render.postgresql;

import io.sqm.core.UpdateStatement;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.defaults.DefaultSqlWriter;
import io.sqm.render.postgresql.spi.PostgresDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.id;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.tbl;
import static io.sqm.dsl.Dsl.update;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UpdateStatementRendererTest {

    @Test
    void rendersUpdateFromWithWhere() {
        var ctx = RenderContext.of(new PostgresDialect());
        UpdateStatement statement = update("users")
            .set(id("name"), col("src", "name"))
            .from(tbl("source_users").as("src"))
            .where(col("users", "id").eq(col("src", "id")))
            .build();

        var sql = normalize(ctx.render(statement).sql());

        assertEquals("UPDATE users SET name = src.name FROM source_users AS src WHERE users.id = src.id", sql);
    }

    @Test
    void rendersUpdateReturning() {
        var ctx = RenderContext.of(new PostgresDialect());
        UpdateStatement statement = update("users")
            .set(id("name"), lit("alice"))
            .where(col("id").eq(lit(1)))
            .result(col("id").toSelectItem(), col("name").toSelectItem())
            .build();

        var sql = normalize(ctx.render(statement).sql());

        assertEquals("UPDATE users SET name = 'alice' WHERE id = 1 RETURNING id, name", sql);
    }

    @Test
    void rendersUpdateWithoutFrom() {
        var ctx = RenderContext.of(new PostgresDialect());
        UpdateStatement statement = update("users")
            .set(id("name"), lit("alice"))
            .build();

        var sql = normalize(ctx.render(statement).sql());

        assertEquals("UPDATE users SET name = 'alice'", sql);
    }

    @Test
    void rejectsUpdateFromWhenDialectDoesNotSupportIt() {
        var renderer = new UpdateStatementRenderer();
        var ansiCtx = RenderContext.of(new io.sqm.render.ansi.spi.AnsiDialect());
        var writer = new DefaultSqlWriter(ansiCtx);
        UpdateStatement statement = update("users")
            .set(id("name"), col("src", "name"))
            .from(tbl("source_users").as("src"))
            .build();

        assertThrows(UnsupportedDialectFeatureException.class, () -> renderer.render(statement, ansiCtx, writer));
    }

    @Test
    void rejectsUpdateReturningWhenDialectDoesNotSupportIt() {
        var renderer = new UpdateStatementRenderer();
        var ansiCtx = RenderContext.of(new io.sqm.render.ansi.spi.AnsiDialect());
        var writer = new DefaultSqlWriter(ansiCtx);
        UpdateStatement statement = update("users")
            .set(id("name"), lit("alice"))
            .result(col("id").toSelectItem())
            .build();

        assertThrows(UnsupportedDialectFeatureException.class, () -> renderer.render(statement, ansiCtx, writer));
    }

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
