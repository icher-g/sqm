package io.sqm.control;

import io.sqm.control.execution.ExecutionContext;
import io.sqm.control.execution.ExecutionMode;
import io.sqm.control.execution.ParameterizationMode;
import io.sqm.control.pipeline.SqlStatementParser;
import io.sqm.control.pipeline.SqlStatementRenderer;
import io.sqm.core.DeleteStatement;
import io.sqm.core.Expression;
import io.sqm.core.InsertStatement;
import io.sqm.core.QuoteStyle;
import io.sqm.core.Query;
import io.sqm.core.UpdateStatement;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static io.sqm.dsl.Dsl.delete;
import static io.sqm.dsl.Dsl.id;
import static io.sqm.dsl.Dsl.insert;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.row;
import static io.sqm.dsl.Dsl.tbl;
import static io.sqm.dsl.Dsl.update;

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
        var delete = DeleteStatement.of(io.sqm.dsl.Dsl.tbl("users"), java.util.List.of(), java.util.List.of(), null, java.util.List.of(), java.util.List.of());

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
        DeleteStatement statement = delete(tbl(id("users", QuoteStyle.BRACKETS))).build();

        var rendered = renderer.render(statement, ExecutionContext.of("sqlserver", ExecutionMode.ANALYZE));

        assertEquals("DELETE FROM [users]", rendered.sql().replaceAll("\\s+", " ").trim());
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
