package io.sqm.render.mysql;

import io.sqm.core.UpdateStatement;
import io.sqm.render.SqlWriter;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.mysql.spi.MySqlDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.id;
import static io.sqm.dsl.Dsl.inner;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.tbl;
import static io.sqm.dsl.Dsl.update;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MySqlUpdateStatementRendererTest {

    @Test
    void rendersJoinedUpdateStatement() {
        var statement = update(tbl("users"))
            .join(inner(tbl("orders")).on(col("users", "id").eq(col("orders", "user_id"))))
            .set(id("name"), lit("alice"))
            .where(col("orders", "state").eq(lit("closed")))
            .build();

        var sql = RenderContext.of(new MySqlDialect()).render(statement).sql();

        assertEquals(
            "UPDATE users INNER JOIN orders ON users.id = orders.user_id SET name = 'alice' WHERE orders.state = 'closed'",
            normalize(sql));
    }

    @Test
    void rendersPlainUpdateWithoutJoins() {
        var statement = update(tbl("users"))
            .set(id("name"), lit("alice"))
            .build();

        var renderer = new MySqlUpdateStatementRenderer();
        var ctx = RenderContext.of(new MySqlDialect());
        SqlWriter writer = new io.sqm.render.defaults.DefaultSqlWriter(ctx);

        renderer.render(statement, ctx, writer);

        assertEquals("UPDATE users SET name = 'alice'", normalize(writer.toText(java.util.List.of()).sql()));
    }

    @Test
    void rejectsJoinedUpdateInDialectWithoutCapability() {
        UpdateStatement statement = update(tbl("users"))
            .join(inner(tbl("orders")).on(col("users", "id").eq(col("orders", "user_id"))))
            .set(id("name"), lit("alice"))
            .build();

        var renderer = new MySqlUpdateStatementRenderer();
        var ctx = RenderContext.of(new AnsiDialect());

        assertThrows(io.sqm.core.dialect.UnsupportedDialectFeatureException.class,
            () -> renderer.render(statement, ctx, new io.sqm.render.defaults.DefaultSqlWriter(ctx)));
    }

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}

