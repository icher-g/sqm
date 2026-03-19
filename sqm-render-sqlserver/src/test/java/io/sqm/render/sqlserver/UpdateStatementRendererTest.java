package io.sqm.render.sqlserver;

import io.sqm.core.QuoteStyle;
import io.sqm.core.Statement;
import io.sqm.core.UpdateStatement;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.sqlserver.spi.SqlServerDialect;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class UpdateStatementRendererTest {

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }

    @Test
    void rendersBracketQuotedUpdateStatement() {
        var ctx = RenderContext.of(new SqlServerDialect());
        UpdateStatement statement = update(io.sqm.dsl.Dsl.tbl(id("users", QuoteStyle.BRACKETS)))
            .set(id("name", QuoteStyle.BRACKETS), lit("alice"))
            .where(col(id("id", QuoteStyle.BRACKETS)).eq(lit(1)))
            .build();

        var sql = normalize(ctx.render(statement).sql());

        assertEquals("UPDATE [users] SET [name] = 'alice' WHERE [id] = 1", sql);
    }

    @Test
    void statementRootRenderingSupportsSqlServerUpdate() {
        var ctx = RenderContext.of(new SqlServerDialect());
        Statement statement = update(io.sqm.dsl.Dsl.tbl(id("users", QuoteStyle.BRACKETS)))
            .set(id("name", QuoteStyle.BRACKETS), lit("alice"))
            .build();

        var sql = normalize(ctx.render(statement).sql());

        assertEquals("UPDATE [users] SET [name] = 'alice'", sql);
    }

    @Test
    void rejectsUpdateFromInSqlServerBaseline() {
        var ctx = RenderContext.of(new SqlServerDialect());
        UpdateStatement statement = update("users")
            .set(id("name"), lit("alice"))
            .from(io.sqm.dsl.Dsl.tbl("source_users"))
            .build();

        assertThrows(io.sqm.core.dialect.UnsupportedDialectFeatureException.class, () -> ctx.render(statement));
    }

    @Test
    void rendersUpdateResultIntoClause() {
        var ctx = RenderContext.of(new SqlServerDialect());
        UpdateStatement statement = update("users")
            .set(id("name"), lit("alice"))
            .result(
                resultInto("audit", "old_name", "new_name"),
                deleted("name"),
                inserted("name")
            )
            .where(col("id").eq(lit(1)))
            .build();

        var sql = normalize(ctx.render(statement).sql());

        assertEquals(
            "UPDATE users SET name = 'alice' OUTPUT deleted.name, inserted.name INTO audit (old_name, new_name) WHERE id = 1",
            sql
        );
    }

    @Test
    void rendersUpdateResultExpressionUsingPseudoColumns() {
        var ctx = RenderContext.of(new SqlServerDialect());
        UpdateStatement statement = update("users")
            .set(id("score"), lit(1))
            .result(inserted("score").add(deleted("score")).as("score_sum"))
            .where(col("id").eq(lit(1)))
            .build();

        var sql = normalize(ctx.render(statement).sql());

        assertEquals(
            "UPDATE users SET score = 1 OUTPUT inserted.score + deleted.score AS score_sum WHERE id = 1",
            sql
        );
    }

    @Test
    void sqlServerRegistryProvidesUpdateRenderer() {
        var repo = Renderers.sqlServer();
        assertInstanceOf(UpdateStatementRenderer.class, repo.require(UpdateStatement.class));
    }
}
