package io.sqm.control;

import io.sqm.control.execution.ExecutionContext;
import io.sqm.control.execution.ExecutionMode;
import io.sqm.control.execution.ParameterizationMode;
import io.sqm.control.pipeline.SqlStatementParser;
import io.sqm.control.pipeline.SqlStatementRenderer;
import io.sqm.core.*;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class SqlStatementRendererTest {

    @Test
    void for_dialect_defaults_to_ansi_when_missing() {
        var renderer = SqlStatementRenderer.standard();
        var result = renderer.render(
            Query.select(Expression.literal(1)).build(),
            ExecutionContext.of("ansi", ExecutionMode.ANALYZE)
        );

        assertTrue(result.sql().toLowerCase().contains("select"));
    }

    @Test
    void for_dialect_supports_postgres_alias() {
        var renderer = SqlStatementRenderer.standard();
        var result = renderer.render(
            Query.select(Expression.literal(1)).build(),
            ExecutionContext.of("postgresql", ExecutionMode.ANALYZE)
        );

        assertTrue(result.sql().toLowerCase().contains("select"));
    }

    @Test
    void for_dialect_rejects_unsupported_dialect() {
        assertThrows(IllegalArgumentException.class, () -> SqlStatementRenderer.standard().render(
            Query.select(Expression.literal(1)).build(),
            ExecutionContext.of("oracle", ExecutionMode.ANALYZE)));
    }

    @Test
    void rendersStatementSequenceWithTerminatingSemicolons() {
        var sequence = StatementSequence.of(
            Query.select(Expression.literal(1)).build(),
            Query.select(Expression.literal(2)).build()
        );

        var result = SqlStatementRenderer.standard().render(
            sequence,
            ExecutionContext.of("ansi", ExecutionMode.ANALYZE)
        );

        assertEquals(2, result.sql().chars().filter(ch -> ch == ';').count());
        assertTrue(result.sql().stripTrailing().endsWith(";"));
        assertTrue(result.sql().toLowerCase().contains("select 1"));
        assertTrue(result.sql().toLowerCase().contains("select 2"));
    }

    @Test
    void convenience_factories_and_blank_dialect_are_supported() {
        var query = Query.select(Expression.literal(1)).build();
        var ansiSql = SqlStatementRenderer.standard().render(query, ExecutionContext.of("ansi", ExecutionMode.ANALYZE));
        var postgresSql = SqlStatementRenderer.standard().render(query, ExecutionContext.of("postgresql", ExecutionMode.ANALYZE));

        assertTrue(ansiSql.sql().toLowerCase().contains("select"));
        assertTrue(postgresSql.sql().toLowerCase().contains("select"));
    }

    @Test
    void renders_mysql_delete_statement() {
        var renderer = SqlStatementRenderer.standard();
        var delete = delete(tbl("users")).build();

        var rendered = renderer.render(delete, ExecutionContext.of("mysql", ExecutionMode.ANALYZE));

        assertTrue(rendered.sql().toLowerCase().startsWith("delete"));
    }

    @Test
    void renders_sqlserver_query() {
        var renderer = SqlStatementRenderer.standard();
        var rendered = renderer.render(
            Query.select(Expression.literal(true)).build(),
            ExecutionContext.of("sqlserver", ExecutionMode.ANALYZE)
        );

        assertTrue(rendered.sql().contains("SELECT"));
        assertTrue(rendered.sql().contains("1"));
    }

    @Test
    void renders_sqlserver_advanced_top_percent_query_with_lock_hint() {
        var renderer = SqlStatementRenderer.standard();
        var statement = Query.select(col("u", "id"))
            .from(tbl(id("users", QuoteStyle.BRACKETS)).as(id("u", QuoteStyle.BRACKETS)).withNoLock())
            .top(TopSpec.of(lit(10), true, false))
            .orderBy(order(col("u", "id")))
            .build();

        var rendered = renderer.render(statement, ExecutionContext.of("sqlserver", ExecutionMode.ANALYZE));

        assertEquals(
            "SELECT TOP (10) PERCENT u.id FROM [users] AS [u] WITH (NOLOCK) ORDER BY u.id",
            rendered.sql().replaceAll("\\s+", " ").trim()
        );
    }

    @Test
    void renders_sqlserver_insert_statement() {
        var renderer = SqlStatementRenderer.standard();
        InsertStatement statement = insert(tbl(id("users", QuoteStyle.BRACKETS)))
            .columns(id("id", QuoteStyle.BRACKETS))
            .values(row(lit(1)))
            .build();

        var rendered = renderer.render(statement, ExecutionContext.of("sqlserver", ExecutionMode.ANALYZE));

        assertEquals("INSERT INTO [users] ([id]) VALUES (1)", rendered.sql().replaceAll("\\s+", " ").trim());
    }

    @Test
    void renders_sqlserver_insert_statement_with_output() {
        var renderer = SqlStatementRenderer.standard();
        InsertStatement statement = insert(tbl(id("users", QuoteStyle.BRACKETS)))
            .columns(id("name", QuoteStyle.BRACKETS))
            .result(inserted(id("id", QuoteStyle.BRACKETS)))
            .values(row(lit("alice")))
            .build();

        var rendered = renderer.render(statement, ExecutionContext.of("sqlserver", ExecutionMode.ANALYZE));

        assertEquals(
            "INSERT INTO [users] ([name]) OUTPUT inserted.[id] VALUES ('alice')",
            rendered.sql().replaceAll("\\s+", " ").trim()
        );
    }

    @Test
    void renders_sqlserver_update_statement() {
        var renderer = SqlStatementRenderer.standard();
        UpdateStatement statement = update(tbl(id("users", QuoteStyle.BRACKETS)))
            .set(id("name", QuoteStyle.BRACKETS), lit("alice"))
            .build();

        var rendered = renderer.render(statement, ExecutionContext.of("sqlserver", ExecutionMode.ANALYZE));

        assertEquals("UPDATE [users] SET [name] = 'alice'", rendered.sql().replaceAll("\\s+", " ").trim());
    }

    @Test
    void renders_sqlserver_delete_statement() {
        var renderer = SqlStatementRenderer.standard();
        var statement = delete(tbl(id("users", QuoteStyle.BRACKETS))).build();

        var rendered = renderer.render(statement, ExecutionContext.of("sqlserver", ExecutionMode.ANALYZE));

        assertEquals("DELETE FROM [users]", rendered.sql().replaceAll("\\s+", " ").trim());
    }

    @Test
    void renders_sqlserver_merge_statement_with_top_output_and_by_source_clause() {
        var renderer = SqlStatementRenderer.standard();
        var statement = SqlStatementParser.standard().parse(
            """
                MERGE TOP (10) PERCENT INTO [users] WITH (HOLDLOCK)
                USING [src_users] AS [s]
                ON [users].[id] = [s].[id]
                WHEN MATCHED AND [s].[active] = 1 THEN UPDATE SET [name] = [s].[name]
                WHEN NOT MATCHED BY SOURCE AND [users].[active] = 0 THEN DELETE
                OUTPUT deleted.[id]
                """,
            ExecutionContext.of("sqlserver", ExecutionMode.ANALYZE)
        );

        var rendered = renderer.render(statement, ExecutionContext.of("sqlserver", ExecutionMode.ANALYZE));

        assertEquals(
            "MERGE TOP (10) PERCENT INTO [users] WITH (HOLDLOCK) USING [src_users] AS [s] ON [users].[id] = [s].[id] WHEN MATCHED AND [s].[active] = 1 THEN UPDATE SET [name] = [s].[name] WHEN NOT MATCHED BY SOURCE AND [users].[active] = 0 THEN DELETE OUTPUT deleted.[id]",
            rendered.sql().replaceAll("\\s+", " ").trim()
        );
    }

    @Test
    void renders_bind_params_when_parameterization_mode_is_bind() {
        var query = SqlStatementParser.standard().parse(
            "select * from users where id = 7",
            ExecutionContext.of("postgresql", ExecutionMode.ANALYZE)
        );
        var renderer = SqlStatementRenderer.standard();

        var inline = renderer.render(query, ExecutionContext.of("postgresql", ExecutionMode.ANALYZE));
        var bind = renderer.render(
            query,
            ExecutionContext.of("postgresql", null, null, ExecutionMode.ANALYZE, ParameterizationMode.BIND)
        );

        assertEquals(0, inline.params().size());
        assertTrue(inline.sql().contains("7"));

        assertFalse(bind.params().isEmpty());
        assertTrue(bind.params().contains(7L));
        assertTrue(bind.sql().contains("?"));
    }
}
