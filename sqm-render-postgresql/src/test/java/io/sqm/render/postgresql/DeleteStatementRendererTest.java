package io.sqm.render.postgresql;

import io.sqm.core.DeleteStatement;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.defaults.DefaultSqlWriter;
import io.sqm.render.postgresql.spi.PostgresDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.delete;
import static io.sqm.dsl.Dsl.tbl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeleteStatementRendererTest {

    @Test
    void rendersDeleteUsingWithWhere() {
        var ctx = RenderContext.of(new PostgresDialect());
        DeleteStatement statement = delete("users")
            .using(tbl("source_users").as("src"))
            .where(col("users", "id").eq(col("src", "id")))
            .build();

        var sql = normalize(ctx.render(statement).sql());

        assertEquals("DELETE FROM users USING source_users AS src WHERE users.id = src.id", sql);
    }

    @Test
    void rendersDeleteWithoutUsing() {
        var ctx = RenderContext.of(new PostgresDialect());
        DeleteStatement statement = delete("users")
            .where(col("id").eq(io.sqm.dsl.Dsl.lit(1)))
            .build();

        var sql = normalize(ctx.render(statement).sql());

        assertEquals("DELETE FROM users WHERE id = 1", sql);
    }

    @Test
    void rejectsDeleteUsingWhenDialectDoesNotSupportIt() {
        var renderer = new DeleteStatementRenderer();
        var ansiCtx = RenderContext.of(new io.sqm.render.ansi.spi.AnsiDialect());
        var writer = new DefaultSqlWriter(ansiCtx);
        DeleteStatement statement = delete("users")
            .using(tbl("source_users").as("src"))
            .build();

        assertThrows(UnsupportedDialectFeatureException.class, () -> renderer.render(statement, ansiCtx, writer));
    }

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
