package io.sqm.render.sqlserver;

import io.sqm.core.QuoteStyle;
import io.sqm.core.InsertStatement;
import io.sqm.core.Statement;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.sqlserver.spi.SqlServerDialect;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.id;
import static io.sqm.dsl.Dsl.insert;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.row;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SqlServerInsertStatementRendererTest {

    @Test
    void rendersBracketQuotedInsertStatement() {
        var ctx = RenderContext.of(new SqlServerDialect());
        InsertStatement statement = insert(io.sqm.dsl.Dsl.tbl(id("users", QuoteStyle.BRACKETS)))
            .columns(id("id", QuoteStyle.BRACKETS), id("name", QuoteStyle.BRACKETS))
            .values(row(lit(1), lit("alice")))
            .build();

        var sql = normalize(ctx.render(statement).sql());

        assertEquals("INSERT INTO [users] ([id], [name]) VALUES (1, 'alice')", sql);
    }

    @Test
    void statementRootRenderingSupportsSqlServerInsert() {
        var ctx = RenderContext.of(new SqlServerDialect());
        Statement statement = insert(io.sqm.dsl.Dsl.tbl(id("users", QuoteStyle.BRACKETS)))
            .columns(id("id", QuoteStyle.BRACKETS))
            .values(row(lit(1)))
            .build();

        var sql = normalize(ctx.render(statement).sql());

        assertEquals("INSERT INTO [users] ([id]) VALUES (1)", sql);
    }

    @Test
    void rejectsInsertReturningInSqlServerDialect() {
        var ctx = RenderContext.of(new SqlServerDialect());
        InsertStatement statement = insert("users")
            .values(row(lit(1)))
            .returning(col("id").toSelectItem())
            .build();

        assertThrows(UnsupportedDialectFeatureException.class, () -> ctx.render(statement));
    }

    @Test
    void sqlServerRegistryProvidesInsertRenderer() {
        var repo = Renderers.sqlServer();
        assertInstanceOf(io.sqm.render.ansi.InsertStatementRenderer.class, repo.require(InsertStatement.class));
    }

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
