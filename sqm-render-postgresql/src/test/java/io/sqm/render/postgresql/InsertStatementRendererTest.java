package io.sqm.render.postgresql;

import io.sqm.core.InsertStatement;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.defaults.DefaultSqlWriter;
import io.sqm.render.postgresql.spi.PostgresDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.id;
import static io.sqm.dsl.Dsl.insert;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.row;
import static io.sqm.dsl.Dsl.set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InsertStatementRendererTest {

    @Test
    void rendersInsertReturning() {
        var ctx = RenderContext.of(new PostgresDialect());
        InsertStatement statement = insert("users")
            .values(row(lit(1), lit("alice")))
            .result(col("id").toSelectItem(), col("name").as("user_name"))
            .build();

        var sql = normalize(ctx.render(statement).sql());

        assertEquals("INSERT INTO users VALUES (1, 'alice') RETURNING id, name AS user_name", sql);
    }

    @Test
    void rendersInsertOnConflictDoNothing() {
        var ctx = RenderContext.of(new PostgresDialect());
        InsertStatement statement = insert("users")
            .values(row(lit(1), lit("alice")))
            .onConflictDoNothing(id("id"))
            .build();

        var sql = normalize(ctx.render(statement).sql());

        assertEquals("INSERT INTO users VALUES (1, 'alice') ON CONFLICT (id) DO NOTHING", sql);
    }

    @Test
    void rendersInsertOnConflictDoNothingWithoutTarget() {
        var ctx = RenderContext.of(new PostgresDialect());
        InsertStatement statement = insert("users")
            .values(row(lit(1), lit("alice")))
            .onConflictDoNothing()
            .build();

        var sql = normalize(ctx.render(statement).sql());

        assertEquals("INSERT INTO users VALUES (1, 'alice') ON CONFLICT DO NOTHING", sql);
    }

    @Test
    void rendersInsertOnConflictDoUpdate() {
        var ctx = RenderContext.of(new PostgresDialect());
        InsertStatement statement = insert("users")
            .columns(id("id"), id("name"))
            .values(row(lit(1), lit("alice")))
            .onConflictDoUpdate(java.util.List.of(id("id")), java.util.List.of(set("name", lit("alice2"))), col("id").eq(lit(1)))
            .build();

        var sql = normalize(ctx.render(statement).sql());

        assertEquals("INSERT INTO users (id, name) VALUES (1, 'alice') ON CONFLICT (id) DO UPDATE SET name = 'alice2' WHERE id = 1", sql);
    }

    @Test
    void rendersInsertOnConflictDoUpdateWithoutWhere() {
        var ctx = RenderContext.of(new PostgresDialect());
        InsertStatement statement = insert("users")
            .columns(id("id"), id("name"))
            .values(row(lit(1), lit("alice")))
            .onConflictDoUpdate(java.util.List.of(id("id")), java.util.List.of(set("name", lit("alice2"))))
            .build();

        var sql = normalize(ctx.render(statement).sql());

        assertEquals("INSERT INTO users (id, name) VALUES (1, 'alice') ON CONFLICT (id) DO UPDATE SET name = 'alice2'", sql);
    }

    @Test
    void rendersInsertWithoutReturning() {
        var ctx = RenderContext.of(new PostgresDialect());
        InsertStatement statement = insert("users")
            .values(row(lit(1), lit("alice")))
            .build();

        var sql = normalize(ctx.render(statement).sql());

        assertEquals("INSERT INTO users VALUES (1, 'alice')", sql);
    }

    @Test
    void rejectsReturningWhenDialectDoesNotSupportIt() {
        var renderer = new InsertStatementRenderer();
        var ansiCtx = RenderContext.of(new io.sqm.render.ansi.spi.AnsiDialect());
        var writer = new DefaultSqlWriter(ansiCtx);
        InsertStatement statement = insert("users")
            .values(row(lit(1)))
            .result(col("id").toSelectItem())
            .build();

        assertThrows(UnsupportedDialectFeatureException.class, () -> renderer.render(statement, ansiCtx, writer));
    }

    @Test
    void rejectsOnConflictWhenDialectDoesNotSupportIt() {
        var renderer = new InsertStatementRenderer();
        var ansiCtx = RenderContext.of(new io.sqm.render.ansi.spi.AnsiDialect());
        var writer = new DefaultSqlWriter(ansiCtx);
        InsertStatement statement = insert("users")
            .values(row(lit(1)))
            .onConflictDoNothing(id("id"))
            .build();

        assertThrows(UnsupportedDialectFeatureException.class, () -> renderer.render(statement, ansiCtx, writer));
    }

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
