package io.sqm.control;

import io.sqm.core.Expression;
import io.sqm.core.Query;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlQueryRendererTest {

    @Test
    void for_dialect_defaults_to_ansi_when_missing() {
        var renderer = SqlQueryRenderer.forDialect(null);
        var sql = renderer.render(
            Query.select(Expression.literal(1)),
            ExecutionContext.of("ansi", ExecutionMode.ANALYZE)
        );

        assertTrue(sql.toLowerCase().contains("select"));
    }

    @Test
    void for_dialect_supports_postgres_alias() {
        var renderer = SqlQueryRenderer.forDialect("postgres");
        var sql = renderer.render(
            Query.select(Expression.literal(1)),
            ExecutionContext.of("postgresql", ExecutionMode.ANALYZE)
        );

        assertTrue(sql.toLowerCase().contains("select"));
    }

    @Test
    void for_dialect_rejects_unsupported_dialect() {
        assertThrows(IllegalArgumentException.class, () -> SqlQueryRenderer.forDialect("mysql"));
    }
}
