package io.sqm.render.sqlserver;

import io.sqm.core.QuoteStyle;
import io.sqm.core.InsertStatement;
import io.sqm.core.Statement;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.sqlserver.spi.SqlServerDialect;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
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
    void rendersInsertOutputClause() {
        var ctx = RenderContext.of(new SqlServerDialect());
        InsertStatement statement = insert("users")
            .columns(id("name"))
            .output(output(outputItem(inserted("id"), "user_id")))
            .values(row(lit("alice")))
            .build();

        var sql = normalize(ctx.render(statement).sql());

        assertEquals("INSERT INTO users (name) OUTPUT inserted.id AS user_id VALUES ('alice')", sql);
    }

    @Test
    void rendersInsertOutputExpressionUsingPseudoColumn() {
        var ctx = RenderContext.of(new SqlServerDialect());
        InsertStatement statement = insert("users")
            .columns(id("name"))
            .output(output(outputItem(inserted("id").add(lit(1)), "next_id")))
            .values(row(lit("alice")))
            .build();

        var sql = normalize(ctx.render(statement).sql());

        assertEquals("INSERT INTO users (name) OUTPUT inserted.id + 1 AS next_id VALUES ('alice')", sql);
    }

    @Test
    void rejectsInsertOutputDeletedReference() {
        var ctx = RenderContext.of(new SqlServerDialect());
        InsertStatement statement = insert("users")
            .values(row(lit("alice")))
            .output(output(outputItem(deleted("id"))))
            .build();

        assertThrows(UnsupportedDialectFeatureException.class, () -> ctx.render(statement));
    }

    @Test
    void sqlServerRegistryProvidesInsertRenderer() {
        var repo = Renderers.sqlServer();
        assertInstanceOf(SqlServerInsertStatementRenderer.class, repo.require(InsertStatement.class));
    }

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
