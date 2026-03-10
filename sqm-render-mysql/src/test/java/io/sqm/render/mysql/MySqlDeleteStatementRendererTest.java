package io.sqm.render.mysql;

import io.sqm.core.DeleteStatement;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.mysql.spi.MySqlDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.delete;
import static io.sqm.dsl.Dsl.inner;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.tbl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MySqlDeleteStatementRendererTest {

    @Test
    void rendersDeleteUsingJoinStatement() {
        var statement = delete(tbl("users"))
            .using(tbl("users"))
            .join(inner(tbl("orders")).on(col("users", "id").eq(col("orders", "user_id"))))
            .where(col("orders", "state").eq(lit("closed")))
            .build();

        var sql = RenderContext.of(new MySqlDialect()).render(statement).sql();

        assertEquals(
            "DELETE FROM users USING users INNER JOIN orders ON users.id = orders.user_id WHERE orders.state = 'closed'",
            normalize(sql));
    }

    @Test
    void rejectsDeleteUsingJoinInDialectWithoutCapability() {
        DeleteStatement statement = delete(tbl("users"))
            .using(tbl("users"))
            .join(inner(tbl("orders")).on(col("users", "id").eq(col("orders", "user_id"))))
            .build();

        var renderer = new MySqlDeleteStatementRenderer();
        var ctx = RenderContext.of(new AnsiDialect());

        assertThrows(io.sqm.core.dialect.UnsupportedDialectFeatureException.class,
            () -> renderer.render(statement, ctx, new io.sqm.render.defaults.DefaultSqlWriter(ctx)));
    }

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
