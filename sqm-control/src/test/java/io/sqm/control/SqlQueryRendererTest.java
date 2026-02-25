package io.sqm.control;

import io.sqm.core.Expression;
import io.sqm.core.Query;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SqlQueryRendererTest {

    @Test
    void for_dialect_defaults_to_ansi_when_missing() {
        var renderer = SqlQueryRenderer.standard();
        var result = renderer.render(
            Query.select(Expression.literal(1)).build(),
            ExecutionContext.of("ansi", ExecutionMode.ANALYZE)
        );

        assertTrue(result.sql().toLowerCase().contains("select"));
    }

    @Test
    void for_dialect_supports_postgres_alias() {
        var renderer = SqlQueryRenderer.standard();
        var result = renderer.render(
            Query.select(Expression.literal(1)).build(),
            ExecutionContext.of("postgresql", ExecutionMode.ANALYZE)
        );

        assertTrue(result.sql().toLowerCase().contains("select"));
    }

    @Test
    void for_dialect_rejects_unsupported_dialect() {
        assertThrows(IllegalArgumentException.class, () -> SqlQueryRenderer.standard().render(
            Query.select(Expression.literal(1)).build(),
            ExecutionContext.of("mysql", ExecutionMode.ANALYZE)));
    }

    @Test
    void convenience_factories_and_blank_dialect_are_supported() {
        var query = Query.select(Expression.literal(1)).build();
        var ansiSql = SqlQueryRenderer.standard().render(query, ExecutionContext.of("ansi", ExecutionMode.ANALYZE));
        var postgresSql = SqlQueryRenderer.standard().render(query, ExecutionContext.of("postgresql", ExecutionMode.ANALYZE));

        assertTrue(ansiSql.sql().toLowerCase().contains("select"));
        assertTrue(postgresSql.sql().toLowerCase().contains("select"));
    }

    @Test
    void renders_bind_params_when_parameterization_mode_is_bind() {
        var query = SqlQueryParser.standard().parse(
            "select * from users where id = 7",
            ExecutionContext.of("postgresql", ExecutionMode.ANALYZE)
        );
        var renderer = SqlQueryRenderer.standard();

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
