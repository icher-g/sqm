package io.sqm.render.sqlserver;

import io.sqm.core.DeleteStatement;
import io.sqm.core.QuoteStyle;
import io.sqm.core.Statement;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.sqlserver.spi.SqlServerDialect;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class DeleteStatementRendererTest {

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }

    @Test
    void rendersBracketQuotedDeleteStatement() {
        var ctx = RenderContext.of(new SqlServerDialect());
        DeleteStatement statement = delete(io.sqm.dsl.Dsl.tbl(id("users", QuoteStyle.BRACKETS)))
            .where(col(id("id", QuoteStyle.BRACKETS)).eq(lit(1)))
            .build();

        var sql = normalize(ctx.render(statement).sql());

        assertEquals("DELETE FROM [users] WHERE [id] = 1", sql);
    }

    @Test
    void statementRootRenderingSupportsSqlServerDelete() {
        var ctx = RenderContext.of(new SqlServerDialect());
        Statement statement = delete(io.sqm.dsl.Dsl.tbl(id("users", QuoteStyle.BRACKETS))).build();

        var sql = normalize(ctx.render(statement).sql());

        assertEquals("DELETE FROM [users]", sql);
    }

    @Test
    void rejectsDeleteUsingInSqlServerBaseline() {
        var ctx = RenderContext.of(new SqlServerDialect());
        DeleteStatement statement = delete("users")
            .using(io.sqm.dsl.Dsl.tbl("source_users"))
            .build();

        assertThrows(io.sqm.core.dialect.UnsupportedDialectFeatureException.class, () -> ctx.render(statement));
    }

    @Test
    void rendersDeleteResultClause() {
        var ctx = RenderContext.of(new SqlServerDialect());
        DeleteStatement statement = delete("users")
            .result(deleted("id").as("deleted_id"))
            .where(col("id").eq(lit(1)))
            .build();

        var sql = normalize(ctx.render(statement).sql());

        assertEquals("DELETE FROM users OUTPUT deleted.id AS deleted_id WHERE id = 1", sql);
    }

    @Test
    void rejectsDeleteResultInsertedReference() {
        var ctx = RenderContext.of(new SqlServerDialect());
        DeleteStatement statement = delete("users")
            .result(inserted("id"))
            .where(col("id").eq(lit(1)))
            .build();

        assertThrows(io.sqm.core.dialect.UnsupportedDialectFeatureException.class, () -> ctx.render(statement));
    }

    @Test
    void sqlServerRegistryProvidesDeleteRenderer() {
        var repo = Renderers.sqlServer();
        assertInstanceOf(DeleteStatementRenderer.class, repo.require(DeleteStatement.class));
    }
}
