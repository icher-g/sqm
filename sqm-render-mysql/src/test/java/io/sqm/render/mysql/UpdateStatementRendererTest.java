package io.sqm.render.mysql;

import io.sqm.core.UpdateStatement;
import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.render.SqlWriter;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.mysql.spi.MySqlDialect;
import io.sqm.render.mysql.spi.MySqlOptimizerHintNormalizationPolicy;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.id;
import static io.sqm.dsl.Dsl.inner;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.straight;
import static io.sqm.dsl.Dsl.tbl;
import static io.sqm.dsl.Dsl.update;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UpdateStatementRendererTest {

    @Test
    void rendersJoinedUpdateStatement() {
        var statement = update(tbl("users"))
            .optimizerHint("BKA(users)")
            .join(inner(tbl("orders")).on(col("users", "id").eq(col("orders", "user_id"))))
            .set(id("name"), lit("alice"))
            .where(col("orders", "state").eq(lit("closed")))
            .build();

        var sql = RenderContext.of(new MySqlDialect()).render(statement).sql();

        assertEquals(
            "UPDATE /*+ BKA(users) */ users INNER JOIN orders ON users.id = orders.user_id SET name = 'alice' WHERE orders.state = 'closed'",
            normalize(sql));
    }

    @Test
    void rendersJoinedUpdateWithAliasAndIndexHintsCanonically() {
        var statement = update(tbl("users").as("u").useIndex("idx_users_name"))
            .join(inner(tbl("orders").as("o").addIndexHint(io.sqm.core.Table.IndexHint.force(
                io.sqm.core.Table.IndexHintScope.JOIN,
                java.util.List.of(io.sqm.core.Identifier.of("idx_orders_user")))))
                .on(col("u", "id").eq(col("o", "user_id"))))
            .set(id("name"), lit("alice"))
            .where(col("o", "state").eq(lit("closed")))
            .build();

        var sql = RenderContext.of(new MySqlDialect()).render(statement).sql();

        assertEquals(
            "UPDATE users AS u USE INDEX (idx_users_name) INNER JOIN orders AS o FORCE INDEX FOR JOIN (idx_orders_user) ON u.id = o.user_id SET name = 'alice' WHERE o.state = 'closed'",
            normalize(sql));
    }

    @Test
    void rendersJoinedUpdateWithQualifiedAssignmentTarget() {
        var statement = update(tbl("users").as("u"))
            .join(inner(tbl("orders").as("o")).on(col("u", "id").eq(col("o", "user_id"))))
            .set(io.sqm.core.QualifiedName.of("u", "name"), lit("alice"))
            .where(col("o", "state").eq(lit("closed")))
            .build();

        var sql = RenderContext.of(new MySqlDialect()).render(statement).sql();

        assertEquals(
            "UPDATE users AS u INNER JOIN orders AS o ON u.id = o.user_id SET u.name = 'alice' WHERE o.state = 'closed'",
            normalize(sql));
    }

    @Test
    void rendersStraightJoinedUpdateWithQualifiedAssignmentTarget() {
        var statement = update(tbl("users").as("u"))
            .join(straight(tbl("orders").as("o")).on(col("u", "id").eq(col("o", "user_id"))))
            .set(io.sqm.core.QualifiedName.of("u", "name"), lit("alice"))
            .where(col("o", "state").eq(lit("closed")))
            .build();

        var sql = RenderContext.of(new MySqlDialect()).render(statement).sql();

        assertEquals(
            "UPDATE users AS u STRAIGHT_JOIN orders AS o ON u.id = o.user_id SET u.name = 'alice' WHERE o.state = 'closed'",
            normalize(sql));
    }

    @Test
    void rendersPlainUpdateWithoutJoins() {
        var statement = update(tbl("users"))
            .optimizerHint("MAX_EXECUTION_TIME(1000)")
            .set(id("name"), lit("alice"))
            .build();

        var renderer = new UpdateStatementRenderer();
        var ctx = RenderContext.of(new MySqlDialect());
        SqlWriter writer = new io.sqm.render.defaults.DefaultSqlWriter(ctx);

        renderer.render(statement, ctx, writer);

        assertEquals("UPDATE /*+ MAX_EXECUTION_TIME(1000) */ users SET name = 'alice'", normalize(writer.toText(java.util.List.of()).sql()));
    }

    @Test
    void normalizesUpdateOptimizerHintsWhenPolicyIsEnabled() {
        var statement = update(tbl("users"))
            .optimizerHint("  MAX_EXECUTION_TIME(1000)\n   BKA(users)  ")
            .set(id("name"), lit("alice"))
            .build();

        var sql = RenderContext.of(new MySqlDialect(
            SqlDialectVersion.of(8, 0),
            MySqlOptimizerHintNormalizationPolicy.NORMALIZE_WHITESPACE
        )).render(statement).sql();

        assertEquals(
            "UPDATE /*+ MAX_EXECUTION_TIME(1000) BKA(users) */ users SET name = 'alice'",
            normalize(sql)
        );
    }

    @Test
    void rejectsJoinedUpdateInDialectWithoutCapability() {
        UpdateStatement statement = update(tbl("users"))
            .optimizerHint("BKA(users)")
            .join(inner(tbl("orders")).on(col("users", "id").eq(col("orders", "user_id"))))
            .set(id("name"), lit("alice"))
            .build();

        var renderer = new UpdateStatementRenderer();
        var ctx = RenderContext.of(new AnsiDialect());

        assertThrows(io.sqm.core.dialect.UnsupportedDialectFeatureException.class,
            () -> renderer.render(statement, ctx, new io.sqm.render.defaults.DefaultSqlWriter(ctx)));
    }

    @Test
    void rejectsUpdateOptimizerHintsWithoutCapability() {
        UpdateStatement statement = update(tbl("users"))
            .optimizerHint("BKA(users)")
            .set(id("name"), lit("alice"))
            .build();

        assertThrows(io.sqm.core.dialect.UnsupportedDialectFeatureException.class,
            () -> RenderContext.of(new NoOptimizerHintMySqlDialect()).render(statement));
    }

    @Test
    void rejectsStraightJoinedUpdateWithoutStraightJoinCapability() {
        UpdateStatement statement = update(tbl("users").as("u"))
            .join(straight(tbl("orders").as("o")).on(col("u", "id").eq(col("o", "user_id"))))
            .set(io.sqm.core.QualifiedName.of("u", "name"), lit("alice"))
            .build();

        assertThrows(io.sqm.core.dialect.UnsupportedDialectFeatureException.class,
            () -> RenderContext.of(new NoStraightJoinMySqlDialect()).render(statement));
    }

    @Test
    void rejectsUpdateReturningInMysql80Renderer() {
        UpdateStatement statement = update(tbl("users"))
            .set(id("name"), lit("alice"))
            .result(col("id").toSelectItem())
            .build();

        assertThrows(io.sqm.core.dialect.UnsupportedDialectFeatureException.class,
            () -> RenderContext.of(new MySqlDialect()).render(statement));
    }

    @Test
    void rejectsUpdateReturningInMysql57Renderer() {
        UpdateStatement statement = update(tbl("users"))
            .set(id("name"), lit("alice"))
            .result(col("id").toSelectItem())
            .build();

        assertThrows(io.sqm.core.dialect.UnsupportedDialectFeatureException.class,
            () -> RenderContext.of(new MySqlDialect(SqlDialectVersion.of(5, 7))).render(statement));
    }

    @Test
    void rendersUpdateReturningWhenCapabilityIsEnabled() {
        UpdateStatement statement = update(tbl("users"))
            .set(id("name"), lit("alice"))
            .result(col("id").toSelectItem())
            .build();

        var sql = RenderContext.of(new ReturningMySqlDialect()).render(statement).sql();

        assertEquals("UPDATE users SET name = 'alice' RETURNING id", normalize(sql));
    }

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }

    private static final class ReturningMySqlDialect extends MySqlDialect {
        @Override
        public DialectCapabilities capabilities() {
            var delegate = super.capabilities();
            return feature -> feature == SqlFeature.DML_RESULT_CLAUSE || delegate.supports(feature);
        }
    }

    private static final class NoStraightJoinMySqlDialect extends MySqlDialect {
        @Override
        public DialectCapabilities capabilities() {
            var delegate = super.capabilities();
            return feature -> feature != SqlFeature.STRAIGHT_JOIN && delegate.supports(feature);
        }
    }

    private static final class NoOptimizerHintMySqlDialect extends MySqlDialect {
        @Override
        public DialectCapabilities capabilities() {
            var delegate = super.capabilities();
            return feature -> feature != SqlFeature.OPTIMIZER_HINT_COMMENT && delegate.supports(feature);
        }
    }
}
