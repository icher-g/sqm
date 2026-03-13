package io.sqm.control.pipeline;

import io.sqm.control.execution.ExecutionContext;
import io.sqm.core.Statement;
import io.sqm.core.dialect.SqlDialectId;
import io.sqm.parser.ansi.AnsiSpecs;
import io.sqm.parser.mysql.spi.MySqlSpecs;
import io.sqm.parser.postgresql.spi.PostgresSpecs;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.Specs;

import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Parses SQL text into SQM {@link Statement} models.
 */
@FunctionalInterface
public interface SqlStatementParser {
    /**
     * Creates the default dialect-aware parser used by middleware.
     *
     * <p>The returned parser resolves the parser dialect from {@link ExecutionContext#dialect()} and supports
     * ANSI, MySQL, plus PostgreSQL aliases ({@code postgresql}, {@code postgres}).</p>
     *
     * @return dialect-aware parser
     */
    static SqlStatementParser standard() {
        return dialectAwareIds(Map.of(
            SqlDialectId.ANSI, AnsiSpecs::new,
            SqlDialectId.MYSQL, MySqlSpecs::new,
            SqlDialectId.POSTGRESQL, PostgresSpecs::new
        ));
    }

    private static SqlStatementParser dialectAwareIds(Map<SqlDialectId, Supplier<Specs>> specsByDialect) {
        Objects.requireNonNull(specsByDialect, "specsByDialect must not be null");
        var mappings = Map.copyOf(specsByDialect);
        return (sql, context) -> {
            Objects.requireNonNull(sql, "sql must not be null");
            Objects.requireNonNull(context, "context must not be null");

            var specsFactory = mappings.get(context.dialectId());
            if (specsFactory == null) {
                throw new IllegalArgumentException("Unsupported dialect: " + context.dialect());
            }

            var ctx = ParseContext.of(specsFactory.get());
            var result = ctx.parse(Statement.class, sql);
            if (result.isError() || result.value() == null) {
                throw new IllegalArgumentException(result.errorMessage());
            }
            return result.value();
        };
    }

    /**
     * Creates a dialect-aware parser with custom dialect mappings.
     *
     * @param specsByDialect mapping of normalized dialect names to parser specs factories
     * @return dialect-aware parser
     */
    static SqlStatementParser dialectAware(Map<String, Supplier<Specs>> specsByDialect) {
        Objects.requireNonNull(specsByDialect, "specsByDialect must not be null");
        var normalized = specsByDialect.entrySet().stream()
            .collect(Collectors.toUnmodifiableMap(
                entry -> SqlDialectId.of(entry.getKey()),
                Map.Entry::getValue
            ));
        return dialectAwareIds(normalized);
    }

    /**
     * Parses SQL text into a statement model for the provided execution context.
     *
     * @param sql     input SQL
     * @param context execution context
     * @return parsed statement model
     */
    Statement parse(String sql, ExecutionContext context);
}




