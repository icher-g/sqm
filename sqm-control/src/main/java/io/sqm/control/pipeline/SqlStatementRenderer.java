package io.sqm.control.pipeline;

import io.sqm.control.execution.ExecutionContext;
import io.sqm.core.Node;
import io.sqm.core.Statement;
import io.sqm.core.StatementSequence;
import io.sqm.core.dialect.SqlDialectId;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.mysql.spi.MySqlDialect;
import io.sqm.render.postgresql.spi.PostgresDialect;
import io.sqm.render.spi.ParameterizationMode;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.RenderOptions;
import io.sqm.render.spi.SqlDialect;
import io.sqm.render.sqlserver.spi.SqlServerDialect;

import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Renders SQM {@link Statement} and {@link StatementSequence} models to SQL text for a target execution context.
 */
@FunctionalInterface
public interface SqlStatementRenderer {
    /**
     * Creates the default dialect-aware renderer used by middleware.
     *
     * <p>The returned renderer resolves the render dialect from {@link ExecutionContext#dialect()} and supports
     * ANSI, MySQL, PostgreSQL aliases ({@code postgresql}, {@code postgres}),
     * and SQL Server aliases ({@code sqlserver}, {@code mssql}, {@code tsql}).</p>
     *
     * @return dialect-aware renderer
     */
    static SqlStatementRenderer standard() {
        return dialectAwareIds(Map.of(
            SqlDialectId.ANSI, AnsiDialect::new,
            SqlDialectId.MYSQL, MySqlDialect::new,
            SqlDialectId.POSTGRESQL, PostgresDialect::new,
            SqlDialectId.SQLSERVER, SqlServerDialect::new
        ));
    }

    private static SqlStatementRenderer dialectAwareIds(Map<SqlDialectId, Supplier<SqlDialect>> specsByDialect) {
        Objects.requireNonNull(specsByDialect, "specsByDialect must not be null");
        var mappings = Map.copyOf(specsByDialect);

        return (sql, context) -> {
            Objects.requireNonNull(sql, "sql must not be null");
            Objects.requireNonNull(context, "context must not be null");
            if (!(sql instanceof Statement) && !(sql instanceof StatementSequence)) {
                throw new IllegalArgumentException("Only Statement and StatementSequence nodes can be rendered by SqlStatementRenderer");
            }

            var specsFactory = mappings.get(context.dialectId());
            if (specsFactory == null) {
                throw new IllegalArgumentException("Unsupported dialect: " + context.dialect());
            }

            var ctx = RenderContext.of(specsFactory.get());
            var result = ctx.render(sql, renderOptions(context));

            return StatementRenderResult.of(result.sql(), result.params());
        };
    }

    /**
     * Creates a dialect-aware renderer with custom dialect mappings.
     *
     * @param specsByDialect mapping of normalized dialect names to parser specs factories
     * @return dialect-aware parser
     */
    static SqlStatementRenderer dialectAware(Map<String, Supplier<SqlDialect>> specsByDialect) {
        Objects.requireNonNull(specsByDialect, "specsByDialect must not be null");
        var normalized = specsByDialect.entrySet().stream()
            .collect(Collectors.toUnmodifiableMap(
                entry -> SqlDialectId.of(entry.getKey()),
                Map.Entry::getValue
            ));
        return dialectAwareIds(normalized);
    }

    private static RenderOptions renderOptions(ExecutionContext context) {
        return RenderOptions.of(switch (context.parameterizationMode()) {
            case OFF -> ParameterizationMode.Inline;
            case BIND -> ParameterizationMode.Bind;
        });
    }

    /**
     * Renders a statement or statement-sequence model to SQL text for the provided execution context.
     *
     * @param query   statement or statement-sequence model to render
     * @param context execution context
     * @return rendered SQL text
     */
    StatementRenderResult render(Node query, ExecutionContext context);
}



