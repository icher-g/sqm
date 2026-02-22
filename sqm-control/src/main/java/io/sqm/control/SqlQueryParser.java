package io.sqm.control;

import io.sqm.core.Query;
import io.sqm.parser.ansi.AnsiSpecs;
import io.sqm.parser.postgresql.spi.PostgresSpecs;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.Specs;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Parses SQL text into SQM {@link Query} models.
 */
@FunctionalInterface
public interface SqlQueryParser {
    /**
     * Creates the default dialect-aware parser used by middleware.
     *
     * <p>The returned parser resolves the parser dialect from {@link ExecutionContext#dialect()} and supports
     * ANSI plus PostgreSQL aliases ({@code postgresql}, {@code postgres}).</p>
     *
     * @return dialect-aware parser
     */
    static SqlQueryParser standard() {
        return dialectAware(Map.of(
            "ansi", AnsiSpecs::new,
            "postgresql", PostgresSpecs::new,
            "postgres", PostgresSpecs::new
        ));
    }

    /**
     * Creates a dialect-aware parser with custom dialect mappings.
     *
     * @param specsByDialect mapping of normalized dialect names to parser specs factories
     * @return dialect-aware parser
     */
    static SqlQueryParser dialectAware(Map<String, Supplier<Specs>> specsByDialect) {
        Objects.requireNonNull(specsByDialect, "specsByDialect must not be null");
        var mappings = Map.copyOf(specsByDialect);
        return (sql, context) -> {
            Objects.requireNonNull(sql, "sql must not be null");
            Objects.requireNonNull(context, "context must not be null");

            var specsFactory = mappings.get(context.dialect().toLowerCase(Locale.ROOT));
            if (specsFactory == null) {
                throw new IllegalArgumentException("Unsupported dialect: " + context.dialect());
            }

            var parseContext = ParseContext.of(specsFactory.get());
            var result = parseContext.parse(Query.class, sql);
            if (result.isError() || result.value() == null) {
                throw new IllegalArgumentException(result.errorMessage());
            }
            return result.value();
        };
    }

    /**
     * Parses SQL text into a query model for the provided execution context.
     *
     * @param sql input SQL
     * @param context execution context
     * @return parsed query model
     */
    Query parse(String sql, ExecutionContext context);
}

