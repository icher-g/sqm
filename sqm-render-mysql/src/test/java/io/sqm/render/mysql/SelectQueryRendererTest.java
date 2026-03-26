package io.sqm.render.mysql;

import io.sqm.core.SelectModifier;
import io.sqm.core.SelectQuery;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.dsl.Dsl;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.mysql.spi.MySqlDialect;
import io.sqm.render.mysql.spi.MySqlOptimizerHintNormalizationPolicy;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.statementHint;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SelectQueryRendererTest {

    @Test
    void rendersSqlCalcFoundRowsAndStatementHints() {
        SelectQuery query = SelectQuery.of(
            List.of(Dsl.col("id").toSelectItem()),
            Dsl.tbl("users"),
            List.of(),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            List.of(),
            List.of(SelectModifier.CALC_FOUND_ROWS),
            List.of(statementHint("MAX_EXECUTION_TIME", 1000))
        );

        var sql = RenderContext.of(new MySqlDialect()).render(query).sql();

        assertTrue(sql.startsWith("SELECT /*+ MAX_EXECUTION_TIME(1000) */ SQL_CALC_FOUND_ROWS id"));
        assertTrue(sql.contains("FROM users"));
    }

    @Test
    void preservesStatementHintsByDefault() {
        SelectQuery query = SelectQuery.of(
            List.of(Dsl.col("id").toSelectItem()),
            Dsl.tbl("users"),
            List.of(),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            List.of(),
            List.of(),
            List.of(statementHint("MAX_EXECUTION_TIME", 1000), statementHint("BKA", "users"))
        );

        var sql = RenderContext.of(new MySqlDialect()).render(query).sql();

        assertTrue(sql.startsWith("SELECT /*+ MAX_EXECUTION_TIME(1000) BKA(users) */ id"));
    }

    @Test
    void normalizesStatementHintsWhenPolicyIsEnabled() {
        SelectQuery query = SelectQuery.of(
            List.of(Dsl.col("id").toSelectItem()),
            Dsl.tbl("users"),
            List.of(),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            List.of(),
            List.of(),
            List.of(statementHint("MAX_EXECUTION_TIME", 1000), statementHint("BKA", "users"))
        );

        var sql = RenderContext.of(new MySqlDialect(
            io.sqm.core.dialect.SqlDialectVersion.of(8, 0),
            MySqlOptimizerHintNormalizationPolicy.NORMALIZE_WHITESPACE
        )).render(query).sql();

        assertTrue(sql.startsWith("SELECT /*+ MAX_EXECUTION_TIME(1000) BKA(users) */ id"));
    }

    @Test
    void rejectsCalcFoundRowsInUnsupportedDialect() {
        SelectQuery query = SelectQuery.of(
            List.of(Dsl.col("id").toSelectItem()),
            Dsl.tbl("users"),
            List.of(),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            List.of(),
            List.of(SelectModifier.CALC_FOUND_ROWS),
            List.of()
        );

        var renderer = new SelectQueryRenderer();
        var ctx = RenderContext.of(new AnsiDialect());

        assertThrows(UnsupportedDialectFeatureException.class,
            () -> renderer.render(query, ctx, new io.sqm.render.defaults.DefaultSqlWriter(ctx)));
    }

    @Test
    void rendersStraightJoin() {
        var query = Dsl.select(Dsl.col("u", "id"))
            .from(Dsl.tbl("users").as("u"))
            .join(Dsl.straight(Dsl.tbl("orders").as("o")).on(Dsl.col("u", "id").eq(Dsl.col("o", "user_id"))))
            .build();

        var sql = RenderContext.of(new MySqlDialect()).render(query).sql();

        assertEquals("SELECT u.id FROM users AS u STRAIGHT_JOIN orders AS o ON u.id = o.user_id", normalize(sql));
    }

    @Test
    void rejectsStraightJoinInUnsupportedDialect() {
        var query = Dsl.select(Dsl.col("u", "id"))
            .from(Dsl.tbl("users").as("u"))
            .join(Dsl.straight(Dsl.tbl("orders").as("o")).on(Dsl.col("u", "id").eq(Dsl.col("o", "user_id"))))
            .build();

        var renderer = new SelectQueryRenderer();
        var ctx = RenderContext.of(new AnsiDialect());

        assertThrows(UnsupportedDialectFeatureException.class,
            () -> renderer.render(query, ctx, new io.sqm.render.defaults.DefaultSqlWriter(ctx)));
    }

    @Test
    void targetTypeIsSelectQuery() {
        assertEquals(SelectQuery.class, new SelectQueryRenderer().targetType());
    }

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
