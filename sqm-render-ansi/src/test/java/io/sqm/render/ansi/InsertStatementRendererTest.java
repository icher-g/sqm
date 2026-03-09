package io.sqm.render.ansi;

import io.sqm.core.InsertStatement;
import io.sqm.core.Statement;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.id;
import static io.sqm.dsl.Dsl.insert;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.row;
import static io.sqm.dsl.Dsl.select;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InsertStatementRendererTest {

    @Test
    void rendersInsertValuesStatement() {
        var ctx = RenderContext.of(new io.sqm.render.ansi.spi.AnsiDialect());
        InsertStatement statement = insert("users")
            .columns(id("id"), id("name"))
            .values(row(lit(1), lit("alice")))
            .build();

        var sql = normalize(ctx.render(statement).sql());

        assertEquals("INSERT INTO users (id, name) VALUES (1, 'alice')", sql);
    }

    @Test
    void rendersInsertSelectStatement() {
        var ctx = RenderContext.of(new io.sqm.render.ansi.spi.AnsiDialect());
        InsertStatement statement = insert("users")
            .columns(id("id"))
            .query(select(lit(1)).build())
            .build();

        var sql = normalize(ctx.render(statement).sql());

        assertEquals("INSERT INTO users (id) SELECT 1", sql);
    }

    @Test
    void rejectsInsertReturningInAnsiDialect() {
        var ctx = RenderContext.of(new io.sqm.render.ansi.spi.AnsiDialect());
        InsertStatement statement = insert("users")
            .values(row(lit(1)))
            .returning(col("id").toSelectItem())
            .build();

        assertThrows(UnsupportedDialectFeatureException.class, () -> ctx.render(statement));
    }

    @Test
    void rejectsInsertOnConflictInAnsiDialect() {
        var ctx = RenderContext.of(new io.sqm.render.ansi.spi.AnsiDialect());
        InsertStatement statement = insert("users")
            .values(row(lit(1)))
            .onConflictDoNothing(id("id"))
            .build();

        assertThrows(UnsupportedDialectFeatureException.class, () -> ctx.render(statement));
    }

    @Test
    void statementRootRenderingSupportsInsert() {
        var ctx = RenderContext.of(new io.sqm.render.ansi.spi.AnsiDialect());
        Statement statement = insert("users")
            .columns(id("id"))
            .values(row(lit(1)))
            .build();

        var sql = normalize(ctx.render(statement).sql());

        assertEquals("INSERT INTO users (id) VALUES (1)", sql);
    }

    @Test
    void ansiRegistryProvidesInsertRenderer() {
        var repo = Renderers.ansi();
        assertInstanceOf(InsertStatementRenderer.class, repo.require(InsertStatement.class));
    }

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
