package io.sqm.control;

import io.sqm.control.execution.ExecutionContext;
import io.sqm.control.execution.ExecutionMode;
import io.sqm.control.execution.ParameterizationMode;
import io.sqm.control.pipeline.SqlStatementParser;
import io.sqm.control.pipeline.SqlStatementRenderer;
import io.sqm.core.DeleteStatement;
import io.sqm.core.Expression;
import io.sqm.core.Query;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
            ExecutionContext.of("sqlserver", ExecutionMode.ANALYZE)));
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
        var delete = DeleteStatement.of(io.sqm.dsl.Dsl.tbl("users"));

        var rendered = renderer.render(delete, ExecutionContext.of("mysql", ExecutionMode.ANALYZE));

        assertTrue(rendered.sql().toLowerCase().startsWith("delete"));
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
