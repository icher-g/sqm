package io.sqm.render.mysql;

import io.sqm.core.DeleteStatement;
import io.sqm.core.Identifier;
import io.sqm.core.Table;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.render.SqlWriter;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.mysql.spi.MySqlDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import java.util.List;

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
    void rendersDeleteUsingJoinWithAliasAndIndexHintsCanonically() {
        var target = tbl("users").as("u").useIndex("idx_users_name");
        var using = Table.of(null, Identifier.of("users"), Identifier.of("u"), Table.Inheritance.DEFAULT,
            List.of(Table.IndexHint.use(Table.IndexHintScope.DEFAULT, List.of(Identifier.of("idx_users_name")))));
        var joinedOrders = tbl("orders").as("o").addIndexHint(Table.IndexHint.force(
            Table.IndexHintScope.JOIN,
            List.of(Identifier.of("idx_orders_user"))));

        var statement = delete(target)
            .using(using)
            .join(inner(joinedOrders).on(col("u", "id").eq(col("o", "user_id"))))
            .where(col("o", "state").eq(lit("closed")))
            .build();

        var sql = RenderContext.of(new MySqlDialect()).render(statement).sql();

        assertEquals(
            "DELETE FROM users AS u USE INDEX (idx_users_name) USING users AS u USE INDEX (idx_users_name) INNER JOIN orders AS o FORCE INDEX FOR JOIN (idx_orders_user) ON u.id = o.user_id WHERE o.state = 'closed'",
            normalize(sql));
    }

    @Test
    void rendersPlainDeleteWithoutUsingOrJoins() {
        var statement = delete(tbl("users")).build();

        var renderer = new MySqlDeleteStatementRenderer();
        var ctx = RenderContext.of(new MySqlDialect());
        SqlWriter writer = new io.sqm.render.defaults.DefaultSqlWriter(ctx);

        renderer.render(statement, ctx, writer);

        assertEquals("DELETE FROM users", normalize(writer.toText(java.util.List.of()).sql()));
    }

    @Test
    void rendersDeleteUsingWithoutJoins() {
        var statement = delete(tbl("users"))
            .using(tbl("users"))
            .where(col("users", "id").eq(lit(1)))
            .build();

        var renderer = new MySqlDeleteStatementRenderer();
        var ctx = RenderContext.of(new MySqlDialect());
        SqlWriter writer = new io.sqm.render.defaults.DefaultSqlWriter(ctx);

        renderer.render(statement, ctx, writer);

        assertEquals("DELETE FROM users USING users WHERE users.id = 1", normalize(writer.toText(java.util.List.of()).sql()));
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

    @Test
    void rejectsDeleteJoinWithoutCapabilityEvenWhenUsingIsEmpty() {
        DeleteStatement statement = DeleteStatement.of(
            tbl("users"),
            java.util.List.of(),
            java.util.List.of(inner(tbl("orders")).on(col("users", "id").eq(col("orders", "user_id")))),
            null,
            java.util.List.of());

        var renderer = new MySqlDeleteStatementRenderer();
        var ctx = RenderContext.of(new AnsiDialect());

        assertThrows(io.sqm.core.dialect.UnsupportedDialectFeatureException.class,
            () -> renderer.render(statement, ctx, new io.sqm.render.defaults.DefaultSqlWriter(ctx)));
    }

    @Test
    void rejectsDeleteReturningInMysql80Renderer() {
        DeleteStatement statement = delete(tbl("users"))
            .where(col("users", "id").eq(lit(1)))
            .returning(col("id").toSelectItem())
            .build();

        assertThrows(io.sqm.core.dialect.UnsupportedDialectFeatureException.class,
            () -> RenderContext.of(new MySqlDialect()).render(statement));
    }

    @Test
    void rejectsDeleteReturningInMysql57Renderer() {
        DeleteStatement statement = delete(tbl("users"))
            .where(col("users", "id").eq(lit(1)))
            .returning(col("id").toSelectItem())
            .build();

        assertThrows(io.sqm.core.dialect.UnsupportedDialectFeatureException.class,
            () -> RenderContext.of(new MySqlDialect(SqlDialectVersion.of(5, 7))).render(statement));
    }

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
