package io.sqm.render.sqlserver;

import io.sqm.core.QuoteStyle;
import io.sqm.core.Statement;
import io.sqm.core.UpdateStatement;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.sqlserver.spi.SqlServerDialect;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.id;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.update;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SqlServerUpdateStatementRendererTest {

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
    void sqlServerRegistryProvidesUpdateRenderer() {
        var repo = Renderers.sqlServer();
        assertInstanceOf(io.sqm.render.ansi.UpdateStatementRenderer.class, repo.require(UpdateStatement.class));
    }

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
