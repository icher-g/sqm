package io.sqm.control.pipeline;

import io.sqm.control.execution.ExecutionContext;
import io.sqm.core.Statement;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.mysql.spi.MySqlDialect;
import io.sqm.render.postgresql.spi.PostgresDialect;
import io.sqm.render.spi.ParameterizationMode;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.RenderOptions;
import io.sqm.render.spi.SqlDialect;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Renders SQM {@link Statement} models to SQL text for a target execution context.
 */
@FunctionalInterface
public interface SqlStatementRenderer {
    /**
     * Creates the default dialect-aware renderer used by middleware.
     *
     * <p>The returned renderer resolves the render dialect from {@link ExecutionContext#dialect()} and supports
     * ANSI, MySQL, plus PostgreSQL aliases ({@code postgresql}, {@code postgres}).</p>
     *
     * @return dialect-aware renderer
     */
    static SqlStatementRenderer standard() {
        return dialectAware(Map.of(
            "ansi", AnsiDialect::new,
            "mysql", MySqlDialect::new,
            "postgresql", PostgresDialect::new,
            "postgres", PostgresDialect::new
        ));
    }

    /**
     * Creates a dialect-aware renderer with custom dialect mappings.
     *
     * @param specsByDialect mapping of normalized dialect names to parser specs factories
     * @return dialect-aware parser
     */
    static SqlStatementRenderer dialectAware(Map<String, Supplier<SqlDialect>> specsByDialect) {
        Objects.requireNonNull(specsByDialect, "specsByDialect must not be null");
        var mappings = Map.copyOf(specsByDialect);

        return (sql, context) -> {
            Objects.requireNonNull(sql, "sql must not be null");
            Objects.requireNonNull(context, "context must not be null");

            var specsFactory = mappings.get(context.dialect().toLowerCase(Locale.ROOT));
            if (specsFactory == null) {
                throw new IllegalArgumentException("Unsupported dialect: " + context.dialect());
            }

            var ctx = RenderContext.of(specsFactory.get());
            var result = ctx.render(sql, renderOptions(context));

            return StatementRenderResult.of(result.sql(), result.params());
        };
    }

    private static RenderOptions renderOptions(ExecutionContext context) {
        return RenderOptions.of(switch (context.parameterizationMode()) {
            case OFF -> ParameterizationMode.Inline;
            case BIND -> ParameterizationMode.Bind;
        });
    }

    /**
     * Renders a statement model to SQL text for the provided execution context.
     *
     * @param query   statement model to render
     * @param context execution context
     * @return rendered SQL text
     */
    StatementRenderResult render(Statement query, ExecutionContext context);
}



