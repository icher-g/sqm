package io.sqm.control;

import io.sqm.core.Query;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.postgresql.spi.PostgresDialect;
import io.sqm.render.spi.RenderContext;

import java.util.Locale;

/**
 * Renders SQM {@link Query} models to SQL text for a target execution context.
 */
@FunctionalInterface
public interface SqlQueryRenderer {
    /**
     * Creates a renderer for the selected dialect.
     *
     * <p>If {@code dialect} is {@code null} or blank, ANSI is used by default.</p>
     *
     * @param dialect dialect identifier (for example, {@code ansi}, {@code postgresql}, or {@code postgres})
     * @return renderer for the selected dialect
     */
    static SqlQueryRenderer forDialect(String dialect) {
        String normalized = normalizeDialect(dialect);
        return switch (normalized) {
            case "ansi" -> (query, context) -> RenderContext.of(new AnsiDialect()).render(query).sql();
            case "postgresql", "postgres" -> (query, context) -> RenderContext.of(new PostgresDialect()).render(query).sql();
            default -> throw new IllegalArgumentException("unsupported dialect: " + dialect);
        };
    }

    /**
     * Creates an ANSI renderer.
     *
     * @return ANSI renderer
     */
    static SqlQueryRenderer ansi() {
        return forDialect("ansi");
    }

    /**
     * Creates a PostgreSQL renderer.
     *
     * @return PostgreSQL renderer
     */
    static SqlQueryRenderer postgresql() {
        return forDialect("postgresql");
    }

    private static String normalizeDialect(String dialect) {
        if (dialect == null || dialect.isBlank()) {
            return "ansi";
        }
        return dialect.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Renders a query model to SQL text for the provided execution context.
     *
     * @param query   query model to render
     * @param context execution context
     * @return rendered SQL text
     */
    String render(Query query, ExecutionContext context);
}
