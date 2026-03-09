package io.sqm.render.postgresql;

import io.sqm.core.InsertStatement;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.defaults.DefaultSqlWriter;
import io.sqm.render.postgresql.spi.PostgresDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.insert;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.row;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InsertStatementRendererTest {

    @Test
    void rendersInsertReturning() {
        var ctx = RenderContext.of(new PostgresDialect());
        InsertStatement statement = insert("users")
            .values(row(lit(1), lit("alice")))
            .returning(col("id").toSelectItem(), col("name").as("user_name"))
            .build();

        var sql = normalize(ctx.render(statement).sql());

        assertEquals("INSERT INTO users VALUES (1, 'alice') RETURNING id, name AS user_name", sql);
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
            .returning(col("id").toSelectItem())
            .build();

        assertThrows(UnsupportedDialectFeatureException.class, () -> renderer.render(statement, ansiCtx, writer));
    }

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
