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
 * Default dialect-aware SQL parser for middleware boundary input.
 */
public final class DefaultSqlQueryParser implements SqlQueryParser {
    private final Map<String, Supplier<Specs>> specsByDialect;

    private DefaultSqlQueryParser(Map<String, Supplier<Specs>> specsByDialect) {
        this.specsByDialect = Map.copyOf(specsByDialect);
    }

    /**
     * Creates parser with built-in dialect mappings.
     *
     * @return parser instance
     */
    public static DefaultSqlQueryParser standard() {
        return new DefaultSqlQueryParser(Map.of(
            "ansi", AnsiSpecs::new,
            "postgresql", PostgresSpecs::new,
            "postgres", PostgresSpecs::new
        ));
    }

    /**
     * Parses SQL text for the given context dialect.
     *
     * @param sql input SQL
     * @param context execution context
     * @return parsed query model
     */
    @Override
    public Query parse(String sql, ExecutionContext context) {
        Objects.requireNonNull(sql, "sql must not be null");
        Objects.requireNonNull(context, "context must not be null");

        var specsFactory = specsByDialect.get(context.dialect().toLowerCase(Locale.ROOT));
        if (specsFactory == null) {
            throw new IllegalArgumentException("Unsupported dialect: " + context.dialect());
        }

        var parseContext = ParseContext.of(specsFactory.get());
        var result = parseContext.parse(Query.class, sql);
        if (result.isError() || result.value() == null) {
            throw new IllegalArgumentException(result.errorMessage());
        }
        return result.value();
    }
}

